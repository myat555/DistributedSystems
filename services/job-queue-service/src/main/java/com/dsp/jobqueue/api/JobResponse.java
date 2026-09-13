package com.dsp.jobqueue.api;

import com.dsp.jobqueue.domain.JobPriority;
import com.dsp.jobqueue.domain.JobRecord;
import com.dsp.jobqueue.domain.JobStatus;

import java.time.Instant;

public record JobResponse(
        String id,
        String type,
        String payload,
        JobPriority priority,
        JobStatus status,
        int attempts,
        int maxRetries,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
    public static JobResponse from(JobRecord job) {
        return new JobResponse(job.id, job.type, job.payload, job.priority, job.status,
                job.attempts, job.maxRetries, job.lastError, job.createdAt, job.updatedAt);
    }
}
