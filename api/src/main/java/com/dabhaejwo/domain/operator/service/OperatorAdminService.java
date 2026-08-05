package com.dabhaejwo.domain.operator.service;

import com.dabhaejwo.domain.operator.dto.request.OperatorActiveRequest;
import com.dabhaejwo.domain.operator.dto.request.OperatorCreateRequest;
import com.dabhaejwo.domain.operator.dto.request.OperatorPasswordRequest;
import com.dabhaejwo.domain.operator.dto.request.OperatorUpdateRequest;
import com.dabhaejwo.domain.operator.dto.response.OperatorResponse;
import com.dabhaejwo.domain.operator.dto.response.RolePermissionsResponse;
import com.dabhaejwo.domain.operator.entity.Operator;
import com.dabhaejwo.domain.operator.repository.OperatorRepository;
import com.dabhaejwo.global.audit.AuditAction;
import com.dabhaejwo.global.audit.AuditLogService;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import com.dabhaejwo.global.security.OperatorRole;
import com.dabhaejwo.global.security.Permission;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 운영자 계정 관리.
 *
 * <p><b>삭제가 없다.</b> {@code operators} 는 여섯 테이블이 FK 로 참조하고, 그중
 * {@code audit_logs} 는 수정·삭제 불가에 3년 보존이다. 계정을 지우면 "누가 이 업체에
 * 대리 접속했나"의 답이 사라진다 — 퇴사한 사람의 행적도 남아 있어야 한다.
 * 비활성화만 하며 과거 기록에는 이름이 그대로 남는다.
 *
 * <p><b>개인별 권한을 두지 않는다.</b> 권한은 역할이 정하고 그 매핑은 코드에 있다
 * (admin-console-plan.md §7). 개인별로 열면 누군가 자기에게 감사 기록 열람을 붙일 수 있고,
 * 그러면 감사 체계 자체가 무의미해진다.
 */
@Service
public class OperatorAdminService {

    private final OperatorRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public OperatorAdminService(OperatorRepository repository,
                                PasswordEncoder passwordEncoder,
                                AuditLogService auditLogService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<OperatorResponse> list() {
        return repository.findAllByOrderByActiveDescNameAsc().stream()
                .map(OperatorResponse::from)
                .toList();
    }

    /**
     * 역할별 권한 매트릭스. 화면이 "이 역할을 주면 무엇을 할 수 있는지"를 정확히 보여주도록
     * <b>서버가 코드의 진실을 그대로 내려준다.</b>
     */
    @Transactional(readOnly = true)
    public List<RolePermissionsResponse> rolePermissions() {
        return Arrays.stream(OperatorRole.values())
                .map(role -> new RolePermissionsResponse(
                        role,
                        role.label(),
                        role.permissions().stream()
                                .map(Permission::name)
                                .sorted()
                                .toList()))
                .toList();
    }

    @Transactional
    public OperatorResponse create(OperatorCreateRequest request) {
        AuthPrincipal.Operator actor = CurrentAuth.operator();

        String email = request.email().strip().toLowerCase();
        if (repository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "이미 등록된 이메일입니다: " + email);
        }

        Operator saved = repository.save(Operator.create(
                email,
                request.name().strip(),
                request.role(),
                passwordEncoder.encode(request.password())));

        // 비밀번호는 어디에도 남기지 않는다. 남는 것은 누가 누구에게 무슨 역할을 줬는가다.
        auditLogService.record(actor.operatorId(), AuditAction.OPERATOR_WRITE, null, request.reason(),
                Map.of("action", "CREATE", "email", email, "role", request.role().name()));

        return OperatorResponse.from(saved);
    }

    @Transactional
    public OperatorResponse update(UUID operatorId, OperatorUpdateRequest request) {
        AuthPrincipal.Operator actor = CurrentAuth.operator();
        Operator target = load(operatorId);
        OperatorRole before = target.getRole();

        if (actor.operatorId().equals(operatorId) && request.role() != OperatorRole.OPS_ADMIN) {
            // 자기 역할을 낮추면 그 순간 이 화면에서 쫓겨나고 되돌릴 수 없다.
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "자기 역할은 낮출 수 없습니다. 다른 운영 관리자에게 요청하세요");
        }
        if (before == OperatorRole.OPS_ADMIN && request.role() != OperatorRole.OPS_ADMIN) {
            requireAnotherAdminRemains(operatorId);
        }

        target.edit(request.name().strip(), request.role());

        auditLogService.record(actor.operatorId(), AuditAction.OPERATOR_WRITE, null, request.reason(),
                Map.of("action", "UPDATE", "target", target.getEmail(),
                        "from", before.name(), "to", request.role().name()));

        return OperatorResponse.from(target);
    }

    @Transactional
    public OperatorResponse changePassword(UUID operatorId, OperatorPasswordRequest request) {
        AuthPrincipal.Operator actor = CurrentAuth.operator();
        Operator target = load(operatorId);

        target.changePassword(passwordEncoder.encode(request.password()));

        auditLogService.record(actor.operatorId(), AuditAction.OPERATOR_WRITE, null, request.reason(),
                Map.of("action", "RESET_PASSWORD", "target", target.getEmail()));

        return OperatorResponse.from(target);
    }

    /**
     * 비활성화·복구. 삭제가 아니다.
     *
     * <p>두 가지를 막는다 — 자기 자신을 끄는 것(즉시 로그인 불가)과 마지막 운영 관리자를
     * 끄는 것(아무도 콘솔을 관리할 수 없게 되고 DB 를 직접 만져야 복구된다).
     */
    @Transactional
    public OperatorResponse changeActive(UUID operatorId, OperatorActiveRequest request) {
        AuthPrincipal.Operator actor = CurrentAuth.operator();
        Operator target = load(operatorId);

        if (!request.active()) {
            if (actor.operatorId().equals(operatorId)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "자기 계정은 비활성화할 수 없습니다");
            }
            if (target.getRole() == OperatorRole.OPS_ADMIN) {
                requireAnotherAdminRemains(operatorId);
            }
        }

        target.changeActive(request.active());

        auditLogService.record(actor.operatorId(), AuditAction.OPERATOR_WRITE, null, request.reason(),
                Map.of("action", request.active() ? "ACTIVATE" : "DEACTIVATE",
                        "target", target.getEmail()));

        return OperatorResponse.from(target);
    }

    /**
     * 대상 말고도 살아 있는 운영 관리자가 남는지 확인한다.
     *
     * <p>마지막 한 명을 끄거나 강등하면 콘솔을 관리할 사람이 없어진다. 그 상태는 화면으로
     * 되돌릴 수 없고 DB 를 직접 고쳐야 한다 — 만들지 않는다.
     */
    private void requireAnotherAdminRemains(UUID excludedId) {
        long admins = repository.findAllByOrderByActiveDescNameAsc().stream()
                .filter(Operator::isActive)
                .filter(operator -> operator.getRole() == OperatorRole.OPS_ADMIN)
                .filter(operator -> !operator.getId().equals(excludedId))
                .count();
        if (admins == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "마지막 운영 관리자입니다. 다른 운영 관리자를 먼저 지정하세요");
        }
    }

    private Operator load(UUID operatorId) {
        return repository.findById(operatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OPERATOR_NOT_FOUND));
    }
}
