package com.dabhaejwo.global.storage;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;

import java.io.InputStream;

/**
 * 저장소 설정이 없을 때.
 *
 * <p><b>로컬 디스크로 대체하지 않는다.</b> 그러면 개발 PC 에서는 업로드가 되는 것처럼
 * 보이다가 배포하면 파일이 사라진다 — 실패를 나중으로 미루는 것뿐이다.
 * 지금 명시적으로 거절해서 설정이 빠졌다는 사실을 즉시 드러낸다.
 */
public class UnavailableFileStorage implements FileStorage {

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                "파일 저장소가 설정되지 않았습니다. 관리자에게 문의해 주세요");
    }

    @Override
    public InputStream get(String key) {
        throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                "파일 저장소가 설정되지 않았습니다");
    }

    @Override
    public void delete(String key) {
        // 저장한 적이 없으니 지울 것도 없다.
    }

    @Override
    public boolean available() {
        return false;
    }
}
