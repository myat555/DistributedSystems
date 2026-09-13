package com.dsp.jobqueue.redis;

import com.dsp.jobqueue.domain.JobRecord;
import com.dsp.jobqueue.domain.JobStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * All Redis access for job state (hash per job), the retry-schedule sorted
 * set used for exponential-backoff redelivery, and the dead-letter set.
 */
@Component
public class JobStore {

    private static final String JOB_KEY_PREFIX = "job:";
    private static final String RETRY_ZSET = "jobs:retry-schedule";
    private static final String DLQ_SET = "jobs:dlq";

    private final StringRedisTemplate redis;

    public JobStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void save(JobRecord job) {
        redis.opsForHash().putAll(JOB_KEY_PREFIX + job.id, job.toRedisHash());
    }

    public JobRecord find(String id) {
        return JobRecord.fromRedisHash(redis.opsForHash().entries(JOB_KEY_PREFIX + id));
    }

    public void updateStatus(String id, JobStatus status) {
        redis.opsForHash().put(JOB_KEY_PREFIX + id, "status", status.name());
        redis.opsForHash().put(JOB_KEY_PREFIX + id, "updatedAt", Instant.now().toString());
    }

    public int incrementAttempts(String id) {
        Long attempts = redis.opsForHash().increment(JOB_KEY_PREFIX + id, "attempts", 1);
        redis.opsForHash().put(JOB_KEY_PREFIX + id, "updatedAt", Instant.now().toString());
        return attempts == null ? 0 : attempts.intValue();
    }

    public void recordError(String id, String message) {
        redis.opsForHash().put(JOB_KEY_PREFIX + id, "lastError", message == null ? "" : message);
    }

    /** Schedules a job for redelivery at {@code readyAt} (used by the backoff scheduler). */
    public void scheduleRetry(String id, Instant readyAt) {
        redis.opsForZSet().add(RETRY_ZSET, id, readyAt.toEpochMilli());
    }

    /** Pops every job whose retry time is due, atomically removing it from the schedule. */
    public Set<String> pollDueRetries() {
        Set<String> due = redis.opsForZSet().rangeByScore(RETRY_ZSET, 0, Instant.now().toEpochMilli());
        if (due == null || due.isEmpty()) {
            return Set.of();
        }
        Set<String> copy = new LinkedHashSet<>(due);
        copy.forEach(id -> redis.opsForZSet().remove(RETRY_ZSET, id));
        return copy;
    }

    public void addToDlq(String id) {
        redis.opsForSet().add(DLQ_SET, id);
    }

    public void removeFromDlq(String id) {
        redis.opsForSet().remove(DLQ_SET, id);
    }

    public Set<String> listDlqIds() {
        Set<String> ids = redis.opsForSet().members(DLQ_SET);
        return ids == null ? Set.of() : ids.stream().collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
