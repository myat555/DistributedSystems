"""
Locust load test for chat-service's real-time WebSocket chat endpoint.

Locust's built-in `HttpUser` only understands HTTP(S), so this file defines a plain
`locust.User` subclass that speaks raw WebSocket directly via the `websocket-client`
package, and manually reports round-trip timings into Locust's stats via
`environment.events.request.fire(...)` — this is Locust's documented mechanism for
getting a custom/non-HTTP protocol to show up in the web UI, CSV/headless output, and
the request_type/name breakdown (total requests, failures, average/median/p95/p99
response time, current RPS), exactly like a normal HTTP request would.

Each simulated user:
  1. Opens one WebSocket connection to `/ws/chat/{roomId}`, where roomId is picked
     from a pool of ~50 rooms (not all piled into a single room, to mirror a realistic
     spread of chat traffic across many rooms rather than one hot room).
  2. On a recurring @task, sends a small `{"sender": ..., "text": ...}` JSON message
     containing a unique marker, then waits to see that same message echoed back to it
     over the same socket. The echo only arrives because chat-service republishes every
     inbound message via Redis Pub/Sub and fans it back out to all sessions subscribed
     to that room (including the sender's own session) — so this round-trip time is a
     direct measurement of the Redis-fanout latency path described in
     services/chat-service/README.md, not just a raw TCP echo.

Install:
    pip install locust websocket-client

Run with the web UI (open http://localhost:8089):
    locust -f load-tests/locust/chat_locustfile.py --host ws://localhost:8082

Run headless, ramping to 10,000 concurrent users at 200/s over a 5 minute run:
    locust -f load-tests/locust/chat_locustfile.py --host ws://localhost:8082 \\
        --headless -u 10000 -r 200 -t 5m --csv=load-tests/jmeter/results/chat

HONEST CAVEAT: getting to a genuine 10,000 concurrent *OS-level* WebSocket connections
requires Locust's distributed mode (one `--master` plus several `--worker` processes,
likely spread across multiple machines) and OS-level tuning on whichever machine(s)
generate the load (raised open-file-descriptor limits, enough ephemeral local ports,
enough free memory/CPU for that many greenlets and socket buffers). Locust's gevent
concurrency model makes thousands of lightweight greenlets per single process feasible
(that's how one process can plausibly emulate a few thousand users at once), but the
practical ceiling for a single-process run is set by the load-generating machine's
resources — not by chat-service, which is the thing actually under test. Running a
single `locust` process against `-u 10000` on a laptop will bottleneck on the load
generator itself long before it proves anything about chat-service's real capacity;
treat the headless command above as the target topology to run distributed, not as
something to expect to succeed unmodified on one box.
"""

import json
import random
import time
import uuid

from locust import User, between, task
from websocket import create_connection

NUM_ROOMS = 50
ROOM_IDS = [f"room-{i}" for i in range(NUM_ROOMS)]
RECEIVE_TIMEOUT_SECONDS = 5


class ChatUser(User):
    """One simulated chat participant holding a single persistent WebSocket connection."""

    wait_time = between(1, 5)

    def on_start(self):
        self.room_id = random.choice(ROOM_IDS)
        self.username = f"user-{uuid.uuid4().hex[:8]}"
        self.ws = None
        self._connect()

    def on_stop(self):
        self._close()

    def _connect(self):
        host = self.host or "ws://localhost:8082"
        url = f"{host}/ws/chat/{self.room_id}"
        start = time.time()
        try:
            # websocket-client's sockets go through the stdlib `socket` module, which
            # Locust's gevent monkey-patching makes cooperative, so this blocking-looking
            # call doesn't actually block the other greenlets/simulated users sharing
            # this process.
            self.ws = create_connection(url, timeout=RECEIVE_TIMEOUT_SECONDS)
            self._report("chat_connect", start, response_length=0, exception=None)
        except Exception as e:
            self.ws = None
            self._report("chat_connect", start, response_length=0, exception=e)

    def _close(self):
        if self.ws is not None:
            try:
                self.ws.close()
            except Exception:
                pass
            self.ws = None

    @task
    def send_and_await_echo(self):
        if self.ws is None:
            self._connect()
            if self.ws is None:
                # Couldn't (re)connect; _connect() already reported the failure.
                return

        marker = uuid.uuid4().hex
        payload = json.dumps({"sender": self.username, "text": f"ping-{marker}"})
        start = time.time()
        try:
            self.ws.send(payload)

            # Read frames until we see our own message echoed back via the Redis
            # fanout. In a room with other simulated users, unrelated messages from
            # other senders may interleave on this same socket, so keep reading
            # (bounded by an overall deadline) rather than trusting the first frame.
            deadline = start + RECEIVE_TIMEOUT_SECONDS
            reply = None
            while True:
                remaining = deadline - time.time()
                if remaining <= 0:
                    raise TimeoutError(f"no echo for our message within {RECEIVE_TIMEOUT_SECONDS}s")
                self.ws.settimeout(remaining)
                reply = self.ws.recv()
                if marker in reply:
                    break

            self._report("chat_roundtrip", start, response_length=len(reply), exception=None)
        except Exception as e:
            self._report("chat_roundtrip", start, response_length=0, exception=e)
            # The socket is presumably in a bad state (timeout, reset, closed by
            # server, etc). Drop it and let the next task invocation reconnect,
            # rather than letting the exception propagate and kill this simulated
            # user's greenlet (or the whole Locust worker process).
            self._close()

    def _report(self, name, start_time, response_length, exception):
        self.environment.events.request.fire(
            request_type="WS",
            name=name,
            response_time=(time.time() - start_time) * 1000,
            response_length=response_length,
            exception=exception,
        )
