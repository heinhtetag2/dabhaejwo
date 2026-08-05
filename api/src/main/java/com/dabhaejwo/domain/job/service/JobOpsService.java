package com.dabhaejwo.domain.job.service;

import com.dabhaejwo.domain.job.dto.response.JobResponse;
import com.dabhaejwo.domain.job.dto.response.JobStatsResponse;
import com.dabhaejwo.domain.job.entity.Job;
import com.dabhaejwo.domain.job.entity.JobStatus;
import com.dabhaejwo.domain.job.repository.JobRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 작업 큐 조회와 재시도.
 *
 * <p><b>재시도는 지금 동작하지 않는다.</b> 상태를 {@code QUEUED} 로 되돌려도 집어갈
 * 워커가 없다. 조용히 성공시키면 운영자는 복구된 줄 알고 기다리므로 명시적으로 거절한다
 * ({@code FEATURE_NOT_READY}) — {@code /api/app/knowledge/**} 의 recrawl·retry 와 같은 정책이다.
 */
@Service
public class JobOpsService {

    private final JobRepository jobRepository;
    private final TenantRepository tenantRepository;

    public JobOpsService(JobRepository jobRepository, TenantRepository tenantRepository) {
        this.jobRepository = jobRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> list(JobStatus status, int page, Integer size) {
        Page<Job> jobs = jobRepository.search(status,
                PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size)));

        Map<UUID, String> tenantNames = tenantRepository
                .findAllById(jobs.getContent().stream().map(Job::getTenantId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName));

        return PageResponse.of(jobs, job -> JobResponse.of(job,
                new JobResponse.TenantRef(job.getTenantId(),
                        tenantNames.getOrDefault(job.getTenantId(), "(삭제된 업체)"))));
    }

    @Transactional(readOnly = true)
    public JobStatsResponse stats() {
        OffsetDateTime todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();

        long done = jobRepository.countByStatusAndUpdatedAtGreaterThanEqual(JobStatus.DONE, todayStart);
        long failedToday = jobRepository.countByStatusAndUpdatedAtGreaterThanEqual(JobStatus.FAILED, todayStart);
        long processedToday = done + failedToday;

        return new JobStatsResponse(
                jobRepository.countByStatus(JobStatus.QUEUED),
                jobRepository.countByStatus(JobStatus.RUNNING),
                done,
                // 오늘 처리된 작업이 없으면 성공률이 정의되지 않는다. 0% 로 내려보내면 전부 실패한 것처럼 보인다.
                processedToday == 0 ? null : Math.round(done * 1000.0 / processedToday) / 10.0,
                jobRepository.countByStatus(JobStatus.FAILED));
    }

    /**
     * 재시도. 지금은 언제나 거절한다.
     *
     * <p>대상이 없으면 그것부터 알린다 — 존재하지 않는 작업에 대해 "기능 미연동"이라고
     * 답하면 운영자가 잘못된 것을 고치려 한다.
     */
    @Transactional(readOnly = true)
    public void retry(Long jobId) {
        jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEATURE_NOT_READY,
                        "해당 작업을 찾을 수 없습니다"));
        // TODO(stub): 임베딩 워커·크롤러 미구현. 큐에 다시 넣어도 집어갈 주체가 없다.
        throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                "재시도는 임베딩 워커·크롤러가 연결된 뒤에 동작합니다");
    }

    @Transactional(readOnly = true)
    public void retryAll() {
        // TODO(stub): 위와 같음.
        throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                "재시도는 임베딩 워커·크롤러가 연결된 뒤에 동작합니다");
    }
}
