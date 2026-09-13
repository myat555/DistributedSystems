package com.dsp.jobqueue.service;

import com.dsp.jobqueue.api.JobRequest;
import com.dsp.jobqueue.api.JobResponse;
import com.dsp.jobqueue.domain.JobMessage;
import com.dsp.jobqueue.domain.JobRecord;
import com.dsp.jobqueue.domain.JobStatus;
import com.dsp.jobqueue.kafka.JobProducer;
import com.dsp.jobqueue.redis.JobStore;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JobService {

    private static final int DEFAULT_MAX_RETRIES = 5;

    private final JobStore jobStore;
    private final JobProducer jobProducer;

    public JobService(JobStore jobStore, JobProducer jobProducer) {
        this.jobStore = jobStore;
        this.jobProducer = jobProducer;
    }

    public JobResponse submit(JobRequest request) {
        JobRecord job = new JobRecord();
        job.id = UUID.randomUUID().toString();
        job.type = request.type();
        job.payload = request.payload();
        job.priority = request.priority();
        job.status = JobStatus.QUEUED;
        job.attempts = 0;
        job.maxRetries = request.maxRetries() != null ? request.maxRetries() : DEFAULT_MAX_RETRIES;
        job.createdAt = Instant.now();
        job.updatedAt = job.createdAt;

        jobStore.save(job);
        jobProducer.publish(job.priority.topic(), new JobMessage(job.id, job.type, job.payload, job.priority, 0));
        return JobResponse.from(job);
    }

    public JobResponse get(String id) {
        JobRecord job = jobStore.find(id);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No job with id " + id);
        }
        return JobResponse.from(job);
    }

    public List<JobResponse> listDeadLettered() {
        return jobStore.listDlqIds().stream()
                .map(jobStore::find)
                .filter(j -> j != null)
                .map(JobResponse::from)
                .toList();
    }

    public JobResponse replay(String id) {
        JobRecord job = jobStore.find(id);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No job with id " + id);
        }
        if (job.status != JobStatus.DEAD_LETTER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job " + id + " is not in the dead-letter queue");
        }
        job.attempts = 0;
        job.status = JobStatus.QUEUED;
        job.lastError = null;
        job.updatedAt = Instant.now();
        jobStore.save(job);
        jobStore.removeFromDlq(id);
        jobProducer.publish(job.priority.topic(), new JobMessage(job.id, job.type, job.payload, job.priority, 0));
        return JobResponse.from(job);
    }
}
