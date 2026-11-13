# Redis & Redisson Demo (Spring Boot 3.4, Java 25)

A runnable demo covering every major feature of Redis (via Spring Data Redis)
and Redisson, with one REST controller per feature area, plus notes on the
differences between the two.

## Prerequisites

- Java 25 (this project was built with Amazon Corretto 25). Set `JAVA_HOME`.
- Maven 3.9+
- Podman 4+ (or Docker)

## 1. Start Redis locally with Podman

```bash
# Pull and run Redis 7
podman run -d --name redis-demo -p 6379:6379 docker.io/library/redis:7-alpine

# Verify
podman ps
redis-cli ping        # -> PONG  (if redis-cli installed)
```

Stop / remove later:

```bash
podman stop redis-demo
podman rm redis-demo
```

Optional: persistent volume

```bash
podman run -d --name redis-demo -p 6379:6379 \
  -v redis-data:/data docker.io/library/redis:7-alpine \
  redis-server --appendonly yes
```

## 2. Run the application

```bash
export JAVA_HOME=/path/to/jdk25
mvn spring-boot:run
```

The app starts on http://localhost:8080. Redis connection is configured in
`src/main/resources/application.yml` (localhost:6379).

## 3. Feature map

### Redis (Spring Data Redis) controllers

| Controller | Base path | Redis types / commands |
|---|---|---|
| RedisStringController | `/api/redis/strings` | SET/GET, INCR, TTL, MSET |
| RedisHashController | `/api/redis/hashes` | HSET/HGET/HGETALL/HINCRBY |
| RedisListController | `/api/redis/lists` | LPUSH/RPUSH/LPOP/RPOP/LRANGE |
| RedisSetController | `/api/redis/sets` | SADD/SMEMBERS/SISMEMBER/SINTER/SUNION |
| RedisZSetController | `/api/redis/zsets` | ZADD/ZRANGE/ZRANK/ZCOUNT/ZINCRBY |
| RedisGeoController | `/api/redis/geo` | GEOADD/GEOPOS/GEODIST/GEORADIUS |
| RedisBitController | `/api/redis/bits` | SETBIT/GETBIT/BITCOUNT/HyperLogLog PFADD/PFCOUNT |
| RedisStreamController | `/api/redis/streams` | XADD/XREAD/XRANGE |
| RedisPubSubController | `/api/redis/pubsub` | PUBLISH (listener logs messages) |
| RedisTransactionController | `/api/redis/tx` | MULTI/EXEC (WATCH) |
| RedisPipelineController | `/api/redis/pipeline` | pipelined batch |
| RedisLuaController | `/api/redis/lua` | EVAL (atomic script) |
| RedisCacheController | `/api/redis/cache` | Spring Cache abstraction over Redis |

### Redisson controllers

| Controller | Base path | Redisson features |
|---|---|---|
| RedissonCollectionsController | `/api/redisson/collections` | RBucket, RMap, RList, RSet, RSortedSet, RQueue, RDeque, RBlockingQueue, RScoredSortedSet, ListMultimap |
| RedissonLockController | `/api/redisson/locks` | RLock, RReadWriteLock, RSemaphore, RCountDownLatch |
| RedissonAdvancedController | `/api/redisson/advanced` | RBitSet, RHyperLogLog, RGeo, RStream, RBloomFilter, RTomStore, RExecutorService, RScheduledExecutorService, RRateLimiter, RAtomicLong, RAtomicDouble |
| RedissonMessagingController | `/api/redisson/messaging` | RTopic pub/sub, RQueue (reliable) |

## 4. Redis vs Redisson

- **Redis (Spring Data Redis)** exposes Redis commands almost 1:1 through
  `RedisTemplate`/`*Operations`. You think in keys, hashes, lists, sets,
  streams, and Lua scripts. Best when you want direct control, raw
  performance, or need a specific Redis command.
- **Redisson** is a higher-level distributed objects framework built on Redis.
  It maps Java collections (`Map`, `List`, `Lock`, `Semaphore`, `ExecutorService`)
  to Redis-backed distributed objects, handling serialization, locking,
  expiration, and failover semantics. Best when you want to share state between
  JVMs with familiar Java APIs.
- **Locks**: Redis has no built-in distributed lock command; Redlock/SET NX EX
  is hand-rolled. Redisson provides `RLock` (reentrant, lease time, watchdog
  auto-renewal) out of the box.
- **Messaging**: Redis pub/sub is fire-and-forget. Redisson adds reliable
  queues, delayed queues, and topic abstractions on top.
- **Cache**: Spring's `@Cacheable` over Redis (via RedisCacheManager) works
  with both; Redisson also offers `RMapCache` with per-entry TTL.

## 5. Notes

- Values are JSON-serialized (`GenericJackson2JsonRedisSerializer`) so they are
  readable with `redis-cli`.
- Keys are kept as plain strings (no type prefix) for easy inspection.
- Pub/sub messages are logged by `RedisPubSubConfig` message listener.
- Cache demo: `GET /api/redis/cache/users/{id}` returns cached value; second
  call is served from Redis (see logs).
