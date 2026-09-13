"""
Load test for the Job Queue Service.

Hammers POST /api/jobs at whatever concurrency you configure and lets Locust's
built-in stats give you throughput, average/median/p95/p99 latency, and error
rate. Point it at the gateway or directly at the service.

Run (through the API gateway, once it's built):
    locust -f load-tests/locust/job_queue_locustfile.py --host http://localhost:8080

Run (directly against job-queue-service, no gateway needed):
    locust -f load-tests/locust/job_queue_locustfile.py --host http://localhost:8081

Then open http://localhost:8089, set user count / spawn rate, and start the run.
Headless example (1000 users, 50/s ramp-up, 5 minute run):
    locust -f load-tests/locust/job_queue_locustfile.py --host http://localhost:8081 \
        --headless -u 1000 -r 50 -t 5m --csv=load-tests/jmeter/results/job_queue
"""

import random
import uuid

from locust import HttpUser, task, between


PRIORITIES = ["HIGH", "MEDIUM", "LOW"]
JOB_TYPES = ["send-email", "generate-report", "resize-image", "sync-crm-record"]


class JobSubmitter(HttpUser):
    # Small think time between submissions per simulated user so we generate
    # sustained concurrent load rather than one giant burst.
    wait_time = between(0.05, 0.5)

    @task(6)
    def submit_job(self):
        payload = {
            "type": random.choice(JOB_TYPES),
            "payload": f"payload-{uuid.uuid4()}",
            "priority": random.choices(PRIORITIES, weights=[2, 5, 3])[0],
            "maxRetries": 5,
        }
        with self.client.post("/api/jobs", json=payload, catch_response=True) as resp:
            if resp.status_code == 202:
                resp.success()
            else:
                resp.failure(f"unexpected status {resp.status_code}: {resp.text}")

    @task(1)
    def check_dlq(self):
        self.client.get("/api/jobs/dlq", name="/api/jobs/dlq")
