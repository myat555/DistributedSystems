package com.dsp.jobqueue.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** In-memory view of a job's Redis hash (key {@code job:<id>}). */
public class JobRecord {

    public String id;
    public String type;
    public String payload;
    public JobPriority priority;
    public JobStatus status;
    public int attempts;
    public int maxRetries;
    public String lastError;
    public Instant createdAt;
    public Instant updatedAt;

    public Map<String, String> toRedisHash() {
        Map<String, String> h = new HashMap<>();
        h.put("id", id);
        h.put("type", type);
        h.put("payload", payload == null ? "" : payload);
        h.put("priority", priority.name());
        h.put("status", status.name());
        h.put("attempts", String.valueOf(attempts));
        h.put("maxRetries", String.valueOf(maxRetries));
        h.put("lastError", lastError == null ? "" : lastError);
        h.put("createdAt", createdAt.toString());
        h.put("updatedAt", updatedAt.toString());
        return h;
    }

    public static JobRecord fromRedisHash(Map<Object, Object> h) {
        if (h == null || h.isEmpty()) {
            return null;
        }
        JobRecord r = new JobRecord();
        r.id = str(h, "id");
        r.type = str(h, "type");
        r.payload = str(h, "payload");
        r.priority = JobPriority.valueOf(str(h, "priority"));
        r.status = JobStatus.valueOf(str(h, "status"));
        r.attempts = Integer.parseInt(str(h, "attempts"));
        r.maxRetries = Integer.parseInt(str(h, "maxRetries"));
        r.lastError = str(h, "lastError");
        r.createdAt = Instant.parse(str(h, "createdAt"));
        r.updatedAt = Instant.parse(str(h, "updatedAt"));
        return r;
    }

    private static String str(Map<Object, Object> h, String key) {
        Object v = h.get(key);
        return v == null ? null : v.toString();
    }
}
