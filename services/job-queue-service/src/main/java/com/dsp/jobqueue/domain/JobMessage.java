package com.dsp.jobqueue.domain;

/** Wire format published to the priority Kafka topics and consumed back off them. */
public record JobMessage(String jobId, String type, String payload, JobPriority priority, int attempt) {
}
