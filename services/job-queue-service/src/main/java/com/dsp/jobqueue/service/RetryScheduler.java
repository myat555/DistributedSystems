package com.dsp.jobqueue.service;

import com.dsp.jobqueue.domain.JobMessage;
import com.dsp.jobqueue.domain.JobRecord;
import com.dsp.jobqueue.domain.JobStatus;
import com.dsp.jobqueue.kafka.JobProducer;
import com.dsp.jobqueue.redis.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Polls the Redis retry-schedule sorted set once a second and republishes any
 * job whose backoff window has elapsed back onto its original priority topic.
 */
@Component
public class RetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetryScheduler.class);

    private final JobStore jobStore;
    private final JobProducer jobProducer;

    public RetryScheduler(JobStore jobStore, JobProducer jobProducer) {
        this.jobStore = jobStore;
        this.jobProducer = jobProducer;
    }

    @Scheduled(fixedDelay = 1000)
    public void requeueDueJobs() {
        Set<String> dueJobIds = jobStore.pollDueRetries();
        for (String jobId : dueJobIds) {
            JobRecord job = jobStore.find(jobId);
            if (job == null) {
                continue;
            }
            jobStore.updateStatus(job.id, JobStatus.QUEUED);
            jobProducer.publish(job.priority.topic(), new JobMessage(job.id, job.type, job.payload, job.priority, job.attempts));
            log.info("Requeued job {} for retry attempt {}", job.id, job.attempts + 1);
        }
    }
}
