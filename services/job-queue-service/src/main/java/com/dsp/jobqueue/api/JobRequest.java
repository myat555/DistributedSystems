package com.dsp.jobqueue.api;

import com.dsp.jobqueue.domain.JobPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobRequest(
        @NotBlank String type,
        String payload,
        @NotNull JobPriority priority,
        Integer maxRetries
) {
}
