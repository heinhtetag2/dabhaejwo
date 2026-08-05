package com.dabhaejwo.domain.operator.service;

import com.dabhaejwo.domain.operator.entity.Operator;
import com.dabhaejwo.domain.operator.repository.OperatorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 운영자 id → 이름. 감사 기록·메모·쿼터 이력이 전부 필요로 한다.
 *
 * <p>목록마다 {@code findById} 를 돌리면 N+1 이 되므로 한 번에 받아 캐시처럼 쓴다.
 *
 * <p>삭제된 운영자를 만나도 예외를 던지지 않는다 — 기록은 3년 남고 사람은 퇴사한다.
 * 이름을 못 찾으면 화면이 id 대신 쓸 수 있는 문구를 준다.
 */
@Service
public class OperatorLookupService {

    private static final String UNKNOWN = "(삭제된 운영자)";

    private final OperatorRepository repository;

    public OperatorLookupService(OperatorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> namesOf(Collection<UUID> operatorIds) {
        if (operatorIds == null || operatorIds.isEmpty()) {
            return Map.of();
        }
        return repository.findAllById(operatorIds.stream().filter(java.util.Objects::nonNull).toList())
                .stream()
                .collect(Collectors.toMap(Operator::getId, Operator::getName));
    }

    public String nameOf(Map<UUID, String> names, UUID operatorId) {
        if (operatorId == null) {
            return null;
        }
        return names.getOrDefault(operatorId, UNKNOWN);
    }
}
