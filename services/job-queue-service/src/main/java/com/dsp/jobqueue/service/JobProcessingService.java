package com.dsp.jobqueue.service;

import com.dsp.jobqueue.domain.JobMessage;
import com.dsp.jobqueue.domain.JobRecord;
import com.dsp.jobqueue.domain.JobStatus;
import com.dsp.jobqueue.kafka.JobProducer;
import com.dsp.jobqueue.redis.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates doing the actual work for a job and implements the
 * retry-with-exponential-backoff / dead-letter failure-recovery policy.
 */
@Service
public class JobProcessingService {

    private static final Logger log = LoggerFactory.getLogger(JobProcessingService.class);

    /** Injected failure rate so the load tests can reliably exercise the retry/DLQ path. */
    private static final double SIMULATED_FAILURE_RATE = 0.15;
    private static final long BASE_BACKOFF_SECONDS = 2;
    private static final long MAX_BACKOFF_SECONDS = 60;

    private final JobStore jobStore;
    private final JobProducer jobProducer;

    public JobProcessingService(JobStore jobStore, JobProducer jobProducer) {
        this.jobStore = jobStore;
        this.jobProducer = jobProducer;
    }

    public void process(JobMessage message) {
        MDC.put("jobId", message.jobId());
        try {
            JobRecord job = jobStore.find(message.jobId());
            if (job == null) {
                log.warn("Received message for unknown job {}, dropping", message.jobId());
                return;
            }
            if (job.status == JobStatus.DEAD_LETTER || job.status == JobStatus.SUCCEEDED) {
                log.info("Job {} already terminal ({}), ignoring duplicate delivery", job.id, job.status);
                return;
            }

            jobStore.updateStatus(job.id, JobStatus.PROCESSING);
            log.info("Processing job {} type={} attempt={}", job.id, job.type, message.attempt());

            try {
                simulateWork(job);
                jobStore.updateStatus(job.id, JobStatus.SUCCEEDED);
                log.info("Job {} succeeded on attempt {}", job.id, message.attempt() + 1);
            } catch (Exception workFailure) {
                handleFailure(job, message, workFailure);
            }
        } finally {
            MDC.remove("jobId");
        }
    }

    private void simulateWork(JobRecord job) throws InterruptedException {
        // Simulated processing latency + a random failure so retries/DLQ are observable.
        Thread.sleep(ThreadLocalRandom.current().nextInt(20, 150));
        if (ThreadLocalRandom.current().nextDouble() < SIMULATED_FAILURE_RATE) {
            throw new RuntimeException("Simulated transient failure while processing job " + job.id);
        }
    }

    private void handleFailure(JobRecord job, JobMessage message, Exception failure) {
        int attempts = jobStore.incrementAttempts(job.id);
        jobStore.recordError(job.id, failure.getMessage());
        log.warn("Job {} failed on attempt {} of {}: {}", job.id, attempts, job.maxRetries, failure.getMessage());

        if (attempts >= job.maxRetries) {
            jobStore.updateStatus(job.id, JobStatus.DEAD_LETTER);
            jobStore.addToDlq(job.id);
            jobProducer.publishToDlq(new JobMessage(job.id, job.type, job.payload, job.priority, attempts));
            log.error("Job {} exhausted {} retries, moved to DLQ", job.id, job.maxRetries);
            return;
        }

        jobStore.updateStatus(job.id, JobStatus.FAILED_RETRYING);
        long backoffSeconds = Math.min(BASE_BACKOFF_SECONDS * (1L << (attempts - 1)), MAX_BACKOFF_SECONDS);
        Instant readyAt = Instant.now().plus(Duration.ofSeconds(backoffSeconds));
        jobStore.scheduleRetry(job.id, readyAt);
        log.info("Job {} scheduled for retry #{} at {} (backoff {}s)", job.id, attempts + 1, readyAt, backoffSeconds);
    }
}
