# Load Testing

Two tools, on purpose: **Locust** (Python, great for WebSocket/custom protocols and
quick iteration) and **JMeter** (industry-standard, GUI + headless, built-in
percentile/error-rate reporting) — between them they cover every project in this repo.

Bring the platform up first: `docker compose up --build` from the repo root.

## What to capture for a resume/portfolio write-up

For each run, note: **throughput** (requests/sec), **average and p95/p99 latency**, and
**error rate** — Locust's web UI and JMeter's Aggregate Report both surface all three
directly. A simple table like this is exactly what an interviewer wants to see:

| Test | Users | Throughput (req/s) | Avg latency | p95 | p99 | Error % |
|---|---|---|---|---|---|---|
| Job Queue (Locust) | 1000 | | | | | |
| Job Queue (JMeter) | 1000 | | | | | |
| Chat (Locust, WS) | 10000 | | | | | |
| Checkout Saga (JMeter) | 500 | | | | | |

Fill this in after running locally — screenshot the Locust charts / JMeter Aggregate
Report table alongside it.

## Locust

```bash
pip install locust websocket-client

# Job Queue: HTTP POST /api/jobs at concurrency
locust -f load-tests/locust/job_queue_locustfile.py --host http://localhost:8081
# headless, 1000 users ramping at 50/s for 5 minutes:
locust -f load-tests/locust/job_queue_locustfile.py --host http://localhost:8081 \
    --headless -u 1000 -r 50 -t 5m --csv=load-tests/jmeter/results/job_queue

# Chat: WebSocket round-trip latency across many rooms
locust -f load-tests/locust/chat_locustfile.py --host ws://localhost:8082
# headless, ramping toward 10,000 concurrent WebSocket users:
locust -f load-tests/locust/chat_locustfile.py --host ws://localhost:8082 \
    --headless -u 10000 -r 200 -t 5m --csv=load-tests/jmeter/results/chat
```

Open http://localhost:8089 for the web UI (live charts + a downloadable HTML report).
Note on the chat test: generating genuinely 10,000 concurrent OS-level WebSocket
connections from one machine needs either Locust's distributed mode (multiple worker
processes, potentially multiple machines) or careful OS file-descriptor/ephemeral-port
tuning on the load-generating box — the ceiling you hit first is very often the load
generator's, not the server's. Document whichever ceiling you actually hit.

## JMeter

Requires JMeter installed locally (`brew install jmeter`, or download from
https://jmeter.apache.org/).

```bash
mkdir -p load-tests/jmeter/results

# Job Queue: throughput/p90/p95/p99/error% via the Aggregate Report
jmeter -n -t load-tests/jmeter/job_queue.jmx \
    -Jhost=localhost -Jport=8081 -Jusers=100 -Jrampup=30 -Jloops=50 \
    -l load-tests/jmeter/results/job_queue_results.jtl

# Checkout Saga: POST /orders then poll GET /orders/{id} to watch it settle,
# proving consistency (CONFIRMED/CANCELLED) even under concurrent load
jmeter -n -t load-tests/jmeter/checkout_saga.jmx \
    -Jhost=localhost -Jport=8083 -Jusers=50 -Jrampup=20 -Jloops=20 -JsagaSettleMs=1500 \
    -l load-tests/jmeter/results/checkout_saga_results.jtl
```

Or open either `.jmx` in the JMeter GUI (`jmeter -t load-tests/jmeter/job_queue.jmx`) to
watch the Aggregate Report populate live and tweak thread-group settings visually.
`-J<name>=<value>` overrides the `${__P(name,default)}` properties defined in each plan
(host/port/users/rampup/loops), so you can point the same `.jmx` at the gateway
(`-Jport=8080`, plus you'd need to add an `Authorization` header via a JMeter Header
Manager once you have a JWT) instead of hitting a service directly.

## Files

- `locust/job_queue_locustfile.py` — HTTP load against the Job Queue.
- `locust/chat_locustfile.py` — WebSocket load against Chat.
- `jmeter/job_queue.jmx` — same target as the Locust job-queue test, JMeter flavor.
- `jmeter/checkout_saga.jmx` — Checkout Saga: submit + poll for saga consistency.
- `jmeter/results/` — `.jtl` output lands here (gitignored).
