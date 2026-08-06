package com.dabhaejwo.domain.branding.controller;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.storage.FileStorage;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;

/**
 * 업체가 올린 브랜딩 이미지를 공개로 내보낸다.
 *
 * <p><b>왜 저장소를 공개로 열지 않았나.</b> R2 버킷에는 업체가 올린 <b>학습 문서</b>가 함께 있다.
 * 버킷을 공개로 돌리면 키를 아는 사람이 남의 회사 내부 문서를 받아갈 수 있다. 공개해도 되는
 * 것만 이 경로로 흘려보낸다.
 *
 * <p><b>키가 내용으로 정해진다</b>({@code ...-{sha256 앞 16자}.png}). 그래서 같은 URL 이
 * 다른 그림을 가리키는 일이 없고, 캐시를 영구로 잡아도 안전하다 — 이미지를 바꾸면 URL 이
 * 바뀌므로 브라우저가 새로 받는다. 위젯이 붙은 사이트의 모든 페이지뷰마다 오는 요청이라
 * 이 캐시가 곧 대역폭이다.
 *
 * <p>경로에 테넌트 id 가 들어가지만 <b>그것으로 권한을 판단하지 않는다.</b> 여기 있는 것은
 * 로고와 아이콘뿐이고 둘 다 방문자에게 보이라고 올린 것이다. 대신 {@code branding/} 아래만
 * 허용해 다른 폴더(문서)로 새어나가지 못하게 막는다.
 */
@RestController
@RequestMapping("/api/public/assets")
public class PublicAssetController {

    /** 이 아래만 공개한다. 학습 문서는 {@code documents/} 라 여기에 걸리지 않는다. */
    private static final String PUBLIC_PREFIX = "branding/";

    private final FileStorage fileStorage;

    public PublicAssetController(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @GetMapping("/tenants/{tenantId}/**")
    public ResponseEntity<InputStreamResource> get(@PathVariable String tenantId,
                                                   jakarta.servlet.http.HttpServletRequest request) {
        String key = keyFrom(request, tenantId);
        InputStream content;
        try {
            content = fileStorage.get(key);
        } catch (RuntimeException e) {
            // 없는 이미지다. 저장소 오류와 구분해 주지 않는다 — 어느 키가 있는지 알려줄 이유가 없다.
            throw new BusinessException(ErrorCode.ASSET_NOT_FOUND);
        }

        return ResponseEntity.ok()
                .contentType(mediaType(key))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(new InputStreamResource(content));
    }

    /**
     * 요청 경로에서 저장소 키를 뽑는다.
     *
     * <p>{@code ..} 가 섞인 경로를 그대로 저장소에 넘기면 다른 테넌트의 폴더로 올라갈 수 있다.
     * 서블릿 컨테이너가 대개 걸러주지만 여기서도 막는다 — 방어는 겹쳐야 한다.
     */
    private String keyFrom(jakarta.servlet.http.HttpServletRequest request, String tenantId) {
        String path = request.getRequestURI();
        int marker = path.indexOf("/api/public/assets/");
        String relative = marker < 0 ? "" : path.substring(marker + "/api/public/assets/".length());

        if (relative.contains("..") || relative.contains("//")) {
            throw new BusinessException(ErrorCode.ASSET_NOT_FOUND);
        }
        String expected = "tenants/" + tenantId + "/" + PUBLIC_PREFIX;
        if (!relative.startsWith(expected)) {
            throw new BusinessException(ErrorCode.ASSET_NOT_FOUND);
        }
        return relative;
    }

    /**
     * 확장자로 판정한다. 업로드할 때 우리가 붙인 확장자이므로 신뢰할 수 있다 —
     * 사용자가 보낸 파일명은 그때 이미 버렸다.
     */
    private MediaType mediaType(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.valueOf("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
