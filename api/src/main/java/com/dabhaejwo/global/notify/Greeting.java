package com.dabhaejwo.global.notify;

/**
 * 메일 인사말에 쓸 이름.
 *
 * <p>이름은 <b>없을 수 있다.</b> 가입 화면은 담당자 이름을 받지 않는다 — 한 화면에 끝내려고
 * 항목을 다섯 개로 줄였기 때문이다(tenant-public-plan.md §4.3). 그 결과 소유자의
 * {@code name} 이 null 이고, 이걸 그대로 찍으면 <b>"null님, 안녕하세요"</b> 가 나간다(실제로 났다).
 *
 * <p>없으면 이메일 앞부분을 쓴다. 이름을 지어내지 않으면서도 본인이 자기 메일임을 알아본다.
 */
public final class Greeting {

    private Greeting() {
    }

    public static String of(String name, String email) {
        if (name != null && !name.isBlank()) {
            return name.strip();
        }
        if (email == null || email.isBlank()) {
            // 여기까지 오면 받는 주소가 없다는 뜻이라 메일 자체가 성립하지 않지만,
            // 인사말 때문에 발송이 터지게 두지는 않는다.
            return "고객";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
