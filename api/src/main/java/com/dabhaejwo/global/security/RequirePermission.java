package com.dabhaejwo.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 선언적 인가. 핸들러마다 if-role 분기를 흩뿌리지 않는다 (core security-rules).
 * 검증은 {@link PermissionAspect} 가 한다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    Permission value();
}
