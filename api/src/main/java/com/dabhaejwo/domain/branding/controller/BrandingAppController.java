package com.dabhaejwo.domain.branding.controller;

import com.dabhaejwo.domain.branding.service.BrandingImageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 로고·런처 아이콘 업로드. 편집 권한이 필요하다({@code BrandingImageService} 가 강제한다).
 *
 * <p>이미지 저장을 {@code PUT /appearance} 와 합치지 않은 이유 — 그쪽은 JSON 이고 이건
 * 멀티파트다. 한 엔드포인트에 섞으면 설정 한 줄을 고칠 때마다 이미지를 다시 올려야 한다.
 */
@RestController
@RequestMapping("/api/app/bots/{botId}/appearance")
public class BrandingAppController {

    private final BrandingImageService service;

    public BrandingAppController(BrandingImageService service) {
        this.service = service;
    }

    @PostMapping("/logo")
    public Map<String, String> uploadLogo(@RequestParam("file") MultipartFile file) {
        return Map.of("url", service.upload(BrandingImageService.Kind.LOGO, file));
    }

    @DeleteMapping("/logo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLogo() {
        service.remove(BrandingImageService.Kind.LOGO);
    }

    @PostMapping("/launcher-icon")
    public Map<String, String> uploadLauncherIcon(@RequestParam("file") MultipartFile file) {
        return Map.of("url", service.upload(BrandingImageService.Kind.LAUNCHER_ICON, file));
    }

    @DeleteMapping("/launcher-icon")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLauncherIcon() {
        service.remove(BrandingImageService.Kind.LAUNCHER_ICON);
    }
}
