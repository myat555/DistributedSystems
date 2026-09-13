# chat-service

Real-time chat backend designed to scale horizontally to 10k+ concurrent users with low
message latency.

## Architecture

Each client holds a plain WebSocket connection (`/ws/chat/{roomId}`, no STOMP/SockJS)
to whichever `chat-service` replica a load balancer happens to route it to. Each
instance keeps an in-memory registry (`SessionRegistry`) of only the sessions it
personally holds, per room — there is no cluster-wide session table and no sticky
sessions. When a message arrives on a socket, three things happen independently: it is
published to a Redis Pub/Sub channel (`chat:{roomId}`), sent asynchronously to the
Kafka topic `chat.messages` keyed by `roomId` (durable history/audit/replay, with
per-room ordering from the Kafka key), and pushed onto a capped Redis list
(`chat:history:{roomId}`, trimmed to 200 entries) so a client that just joined a room
can fetch recent context over REST without replaying Kafka. Presence is tracked
cluster-wide in a Redis Set (`chat:presence:{roomId}`).

**Why Redis Pub/Sub is the key to horizontal scaling:** every `chat-service` instance
subscribes to the `chat:*` pattern on startup. Because *all* instances receive *every*
published message regardless of which instance produced it, each instance can forward
a message to its own local sessions for that room and nothing more needs to be shared
or coordinated. That decouples "who received the message" from "who holds the
recipients' sockets" — the two would otherwise be the same instance, which is exactly
what prevents naive single-instance WebSocket servers from scaling past what one JVM's
socket/thread capacity allows. Adding replicas (see `k8s/chat-service/deployment.yaml`,
which runs 3 by default and autoscales to 12) linearly increases the number of sockets
the system can hold, with Redis Pub/Sub as the (cheap, low-latency) fanout backbone.

## REST endpoints

- `GET /api/chat/rooms/{roomId}/messages?limit=50` — recent history (capped at 200).
- `GET /api/chat/rooms/{roomId}/presence` — `{roomId, count, members}` from the
  cluster-wide presence set.

## Manual test

```
wscat -c ws://localhost:8082/ws/chat/room1
> {"sender":"alice","text":"hello"}
```

Open a second `wscat` connection to the same room in another terminal:

```
wscat -c ws://localhost:8082/ws/chat/room1
```

Sending a message from either terminal should be echoed to both, proving the
Redis fanout path works even before scaling to multiple replicas. Then check history
and presence:

```
curl http://localhost:8082/api/chat/rooms/room1/messages?limit=10
curl http://localhost:8082/api/chat/rooms/room1/presence
```

## Load testing

See `load-tests/locust/chat_locustfile.py` for a Locust-based WebSocket load test that
simulates many concurrent chat users, ramping up to 10,000 concurrent connections
across ~50 rooms.
