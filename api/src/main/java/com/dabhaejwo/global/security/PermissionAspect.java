package com.dabhaejwo.global.security;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requirePermission)")
    public void check(RequirePermission requirePermission) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        if (!operator.can(requirePermission.value())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    operator.role() + " 역할에는 " + requirePermission.value() + " 권한이 없습니다");
        }
    }
}
