package com.dabhaejwo.domain.job.controller;

import com.dabhaejwo.domain.job.dto.response.JobResponse;
import com.dabhaejwo.domain.job.dto.response.JobStatsResponse;
import com.dabhaejwo.domain.job.entity.JobStatus;
import com.dabhaejwo.domain.job.service.JobOpsService;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops/jobs")
public class JobOpsController {

    private final JobOpsService service;

    public JobOpsController(JobOpsService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission(Permission.JOB_READ)
    public PageResponse<JobResponse> list(@RequestParam(required = false) JobStatus status,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(required = false) Integer size) {
        return service.list(status, page, size);
    }

    @GetMapping("/stats")
    @RequirePermission(Permission.JOB_READ)
    public JobStatsResponse stats() {
        return service.stats();
    }

    @PostMapping("/{jobId}/retry")
    @RequirePermission(Permission.JOB_RETRY)
    public void retry(@PathVariable Long jobId) {
        service.retry(jobId);
    }

    @PostMapping("/retry-all")
    @RequirePermission(Permission.JOB_RETRY)
    public void retryAll() {
        service.retryAll();
    }
}
