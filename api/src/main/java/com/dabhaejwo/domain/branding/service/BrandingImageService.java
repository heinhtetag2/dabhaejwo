package com.dabhaejwo.domain.branding.service;

import com.dabhaejwo.domain.botsettings.entity.BotSettings;
import com.dabhaejwo.domain.botsettings.repository.BotSettingsRepository;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.CurrentAuth;
import com.dabhaejwo.global.storage.FileStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 로고·런처 아이콘 업로드.
 *
 * <p>키를 <b>내용의 해시</b>로 만든다({@code branding/logo-{sha256 앞 16자}.png}). 그래서
 * 같은 URL 이 다른 그림을 가리키는 일이 없고, 공개 경로에서 캐시를 영구로 잡아도 안전하다 —
 * 이미지를 바꾸면 URL 이 바뀌므로 브라우저가 새로 받는다.
 *
 * <p><b>옛 파일을 지우지 않는다.</b> 방금까지 그 URL 을 담아 응답한 위젯이 남의 사이트에서
 * 아직 돌고 있고, 지우면 그 페이지의 런처가 깨진 이미지가 된다. 512KB 짜리가 몇 개 남는
 * 비용보다 그쪽이 비싸다.
 */
@Service
public class BrandingImageService {

    public enum Kind {
        LOGO("logo"),
        LAUNCHER_ICON("icon");

        private final String prefix;

        Kind(String prefix) {
            this.prefix = prefix;
        }
    }

    private final BotSettingsRepository settingsRepository;
    private final TenantRepository tenantRepository;
    private final FileStorage fileStorage;

    public BrandingImageService(BotSettingsRepository settingsRepository,
                                TenantRepository tenantRepository,
                                FileStorage fileStorage) {
        this.settingsRepository = settingsRepository;
        this.tenantRepository = tenantRepository;
        this.fileStorage = fileStorage;
    }

    /** @return 저장된 이미지의 공개 경로 */
    @Transactional
    public String upload(Kind kind, MultipartFile file) {
        UUID tenantId = CurrentAuth.requireEditor().tenantId();
        if (!fileStorage.available()) {
            throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                    "파일 저장소가 연결되지 않아 이미지를 올릴 수 없습니다");
        }

        byte[] content = read(file);
        String extension = BrandingImagePolicy.verify(content, file.getOriginalFilename());

        String key = "tenants/%s/branding/%s-%s.%s"
                .formatted(tenantId, kind.prefix, hash(content), extension);
        // 같은 이미지를 다시 올리면 같은 키다. 덮어써도 내용이 같으므로 문제가 없다.
        fileStorage.put(key, new ByteArrayInputStream(content), content.length, contentType(extension));

        String url = "/api/public/assets/" + key;
        apply(tenantId, kind, url);
        return url;
    }

    /** 지우면 기본 아이콘으로 돌아간다. 저장소의 파일은 남긴다(위 주석 참조). */
    @Transactional
    public void remove(Kind kind) {
        apply(CurrentAuth.requireEditor().tenantId(), kind, null);
    }

    private void apply(UUID tenantId, Kind kind, String url) {
        BotSettings settings = settingsRepository.findById(tenantId)
                .orElseGet(() -> settingsRepository.save(BotSettings.defaults(
                        tenantId,
                        tenantRepository.findById(tenantId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND))
                                .getName())));

        settings.changeBranding(
                kind == Kind.LAUNCHER_ICON ? url : settings.getLauncherIconUrl(),
                kind == Kind.LOGO ? url : settings.getLogoUrl());
    }

    private byte[] read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "파일이 없습니다");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "파일을 읽지 못했습니다");
        }
    }

    private String hash(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없습니다", e);
        }
    }

    private String contentType(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}
