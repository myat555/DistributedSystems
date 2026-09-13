package com.dsp.jobqueue.api;

import com.dsp.jobqueue.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> submit(@Valid @RequestBody JobRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobService.submit(request));
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable String id) {
        return jobService.get(id);
    }

    @GetMapping("/dlq")
    public List<JobResponse> deadLettered() {
        return jobService.listDeadLettered();
    }

    @PostMapping("/dlq/{id}/replay")
    public JobResponse replay(@PathVariable String id) {
        return jobService.replay(id);
    }
}
