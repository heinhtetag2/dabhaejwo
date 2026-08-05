package com.dabhaejwo.domain.knowledge.service;

import com.dabhaejwo.domain.knowledge.dto.response.KnowledgeDocumentResponse;
import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeDocument;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeSource;
import com.dabhaejwo.domain.knowledge.entity.SourceType;
import com.dabhaejwo.domain.knowledge.indexing.DocumentIndexer;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeSourceRepository;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.config.AppProperties;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import com.dabhaejwo.global.storage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * 지식 파일 업로드.
 *
 * <p>순서가 중요하다 — <b>검증 → 저장소 업로드 → DB 기록</b>. 반대로 하면 DB 에는 있는데
 * 파일이 없는 문서가 생기고, 화면은 "학습 중"으로 영원히 남는다.
 *
 * <p>글자를 뽑아 학습하는 일은 <b>요청 안에서 하지 않는다.</b> 문서를 {@code PENDING} 으로
 * 두면 {@code IndexingWorker} 가 집어간다. 20MB PDF 를 동기로 처리하면 수십 초가 걸리고
 * 브라우저는 타임아웃으로 끊는데, 그때 사용자는 업로드가 실패한 줄 안다 — 파일은 올라가 있는데도.
 */
@Service
public class DocumentUploadService {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadService.class);

    /** 파일 소스의 표시명. 업체마다 하나씩 자동으로 만든다. */
    private static final String FILE_SOURCE_LABEL = "업로드 파일";

    private final KnowledgeSourceRepository sourceRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final FileStorage fileStorage;
    private final DocumentIndexer indexer;
    private final AppProperties.Storage storageConfig;

    public DocumentUploadService(KnowledgeSourceRepository sourceRepository,
                                 KnowledgeDocumentRepository documentRepository,
                                 TenantRepository tenantRepository,
                                 PlanRepository planRepository,
                                 FileStorage fileStorage,
                                 DocumentIndexer indexer,
                                 AppProperties properties) {
        this.sourceRepository = sourceRepository;
        this.documentRepository = documentRepository;
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.fileStorage = fileStorage;
        this.indexer = indexer;
        this.storageConfig = properties.storage();
    }

    @Transactional
    public KnowledgeDocumentResponse upload(MultipartFile file) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        UUID tenantId = user.tenantId();

        validate(file);
        requireQuota(tenantId);

        byte[] content = read(file);
        String sha256 = sha256(content);

        // 같은 파일을 또 올리면 문서가 두 벌 생기고 한도만 깎인다.
        Optional<KnowledgeDocument> existing =
                documentRepository.findFirstByTenantIdAndContentSha256(tenantId, sha256);
        if (existing.isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "이미 올린 파일입니다: " + existing.get().getTitle());
        }

        KnowledgeSource source = fileSource(tenantId);
        String displayName = UploadPolicy.safeDisplayName(file.getOriginalFilename());
        String contentType = UploadPolicy.contentTypeFor(file.getOriginalFilename());

        KnowledgeDocument document = KnowledgeDocument.of(
                tenantId, source.getId(), displayName, null, DocumentStatus.PENDING);
        // 키에 문서 id 가 필요하므로 먼저 저장해 id 를 받는다.
        documentRepository.save(document);

        String key = storageKey(tenantId, document.getId());
        try (InputStream stream = new java.io.ByteArrayInputStream(content)) {
            fileStorage.put(key, stream, content.length, contentType);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "파일을 읽지 못했습니다");
        }

        document.attachFile(key, contentType, displayName, content.length, sha256);

        log.info("파일을 저장했습니다 — tenant={}, key={}, {}bytes. 학습 대기열에 들어갑니다",
                tenantId, key, content.length);

        return KnowledgeDocumentResponse.from(document);
    }

    /**
     * 문서 삭제. 저장소 오브젝트도 함께 지운다.
     *
     * <p>대리 접속 중에는 막힌다 — 운영자가 남의 자료를 지우는 일은 없어야 한다
     * (tenant-plan.md §6.2 금지 목록).
     */
    @Transactional
    public void delete(UUID documentId) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        CurrentAuth.rejectIfImpersonating();

        KnowledgeDocument document = documentRepository.findById(documentId)
                .filter(found -> found.getTenantId().equals(user.tenantId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 조각을 먼저 지운다. 남으면 없는 문서의 내용이 계속 검색된다.
        indexer.removeChunks(user.tenantId(), documentId);
        if (document.getStorageKey() != null) {
            fileStorage.delete(document.getStorageKey());
        }
        documentRepository.delete(document);
    }

    private void validate(MultipartFile file) {
        if (!fileStorage.available()) {
            throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                    "파일 저장소가 설정되지 않았습니다. 관리자에게 문의해 주세요");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "빈 파일입니다");
        }
        if (file.getSize() > storageConfig.maxFileSizeBytes()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "파일이 너무 큽니다. " + storageConfig.maxFileSizeMb() + "MB 까지 올릴 수 있습니다");
        }
        String filename = file.getOriginalFilename();
        if (!UploadPolicy.allowed(filename)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "지원하지 않는 형식입니다. " + UploadPolicy.allowedExtensionsLabel() + " 만 올릴 수 있습니다");
        }
        if (UploadPolicy.contentTypeConflicts(filename, file.getContentType())) {
            // 확장자와 내용 종류가 어긋난다. 둘 중 하나가 거짓말이므로 받지 않는다.
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "파일 형식이 확장자와 맞지 않습니다");
        }
    }

    /** 요금제 문서 한도. 넘으면 올리기 전에 막는다 — 올리고 나서 지우라고 하면 늦다. */
    private void requireQuota(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        Plan plan = planRepository.findById(tenant.getPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        if (documentRepository.countActive(tenantId) >= plan.getDocLimit()) {
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED,
                    "학습 문서 한도(" + plan.getDocLimit() + "개)에 도달했습니다. "
                            + "요금제를 올리거나 쓰지 않는 문서를 지워 주세요");
        }
    }

    /** 업체별 파일 소스. 없으면 만든다 — 업로드하려고 소스를 먼저 만들게 하지 않는다. */
    private KnowledgeSource fileSource(UUID tenantId) {
        return sourceRepository.findFirstByTenantIdAndType(tenantId, SourceType.FILE)
                .orElseGet(() -> sourceRepository.save(
                        KnowledgeSource.of(tenantId, SourceType.FILE, FILE_SOURCE_LABEL, false)));
    }

    /**
     * 저장소 키. 테넌트를 경로에 넣어 해지 시 접두사로 한 번에 지울 수 있게 한다.
     * 사용자가 올린 파일명은 키에 넣지 않는다 — 경로 조작과 충돌이 동시에 생긴다.
     */
    private String storageKey(UUID tenantId, UUID documentId) {
        return "tenants/" + tenantId + "/documents/" + documentId;
    }

    private byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "파일을 읽지 못했습니다");
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없습니다", e);
        }
    }
}
