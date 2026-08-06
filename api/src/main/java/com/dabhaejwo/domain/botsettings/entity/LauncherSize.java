package com.dabhaejwo.domain.botsettings.entity;

/**
 * 런처(동그란 버블) 크기.
 *
 * <p>픽셀을 직접 받지 않는다. 업체는 자기 사이트에서 이 값이 어떻게 보일지 감으로 정하게
 * 되는데, 그러면 남의 화면을 가릴 만큼 큰 값이나 손가락으로 못 누를 만큼 작은 값이 나온다.
 * 셋 중 하나면 어느 쪽으로도 실패하지 않는다.
 *
 * <p><b>실제 픽셀은 위젯 CSS 가 정한다.</b> 여기 적힌 값은 화면에 설명을 띄우기 위한 것이고,
 * 두 곳이 어긋나면 위젯이 진실이다.
 */
public enum LauncherSize {

    SMALL(48),
    MEDIUM(56),
    LARGE(64);

    private final int px;

    LauncherSize(int px) {
        this.px = px;
    }

    public int px() {
        return px;
    }
}
