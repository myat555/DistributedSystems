package com.dsp.jobqueue.domain;

public enum JobStatus {
    QUEUED,
    PROCESSING,
    SUCCEEDED,
    FAILED_RETRYING,
    DEAD_LETTER
}
