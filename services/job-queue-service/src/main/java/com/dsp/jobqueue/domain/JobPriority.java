package com.dsp.jobqueue.domain;

public enum JobPriority {
    HIGH("jobs.high"),
    MEDIUM("jobs.medium"),
    LOW("jobs.low");

    private final String topic;

    JobPriority(String topic) {
        this.topic = topic;
    }

    public String topic() {
        return topic;
    }
}
