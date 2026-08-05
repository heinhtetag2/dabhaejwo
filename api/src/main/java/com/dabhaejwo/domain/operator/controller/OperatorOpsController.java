package com.dabhaejwo.domain.operator.controller;

import com.dabhaejwo.domain.operator.dto.request.OperatorActiveRequest;
import com.dabhaejwo.domain.operator.dto.request.OperatorCreateRequest;
import com.dabhaejwo.domain.operator.dto.request.OperatorPasswordRequest;
import com.dabhaejwo.domain.operator.dto.request.OperatorUpdateRequest;
import com.dabhaejwo.domain.operator.dto.response.OperatorResponse;
import com.dabhaejwo.domain.operator.dto.response.RolePermissionsResponse;
import com.dabhaejwo.domain.operator.service.OperatorAdminService;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 운영자 계정 관리. <b>{@code OPS_ADMIN} 전용이다.</b>
 *
 * <p><b>DELETE 가 없다.</b> 감사 기록이 행위자를 FK 로 참조하고 3년 보존이라 지울 수 없다.
 * 비활성화({@code PATCH /active})가 그 자리를 대신한다.
 */
@RestController
@RequestMapping("/api/ops/operators")
public class OperatorOpsController {

    private final OperatorAdminService service;

    public OperatorOpsController(OperatorAdminService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission(Permission.OPERATOR_READ)
    public List<OperatorResponse> list() {
        return service.list();
    }

    /** 역할이 무엇을 할 수 있는지. 화면이 매트릭스를 복제하지 않도록 서버가 준다. */
    @GetMapping("/role-permissions")
    @RequirePermission(Permission.OPERATOR_READ)
    public List<RolePermissionsResponse> rolePermissions() {
        return service.rolePermissions();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.OPERATOR_WRITE)
    public OperatorResponse create(@Valid @RequestBody OperatorCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{operatorId}")
    @RequirePermission(Permission.OPERATOR_WRITE)
    public OperatorResponse update(@PathVariable UUID operatorId,
                                   @Valid @RequestBody OperatorUpdateRequest request) {
        return service.update(operatorId, request);
    }

    @PatchMapping("/{operatorId}/password")
    @RequirePermission(Permission.OPERATOR_WRITE)
    public OperatorResponse changePassword(@PathVariable UUID operatorId,
                                           @Valid @RequestBody OperatorPasswordRequest request) {
        return service.changePassword(operatorId, request);
    }

    /** 비활성화·복구. 삭제가 아니다. */
    @PatchMapping("/{operatorId}/active")
    @RequirePermission(Permission.OPERATOR_WRITE)
    public OperatorResponse changeActive(@PathVariable UUID operatorId,
                                         @Valid @RequestBody OperatorActiveRequest request) {
        return service.changeActive(operatorId, request);
    }
}
