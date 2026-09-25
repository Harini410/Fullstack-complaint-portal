# Scalability & High-Throughput System Design

## 1. High-Concurrency Architecture Blueprint

```mermaid
flowchart TD
    Users["Concurrent Users (10,000+ RPS)"] --> CloudFlare["Cloudflare CDN & DDoS Protection"]
    CloudFlare --> ALB["Application Load Balancer (AWS ALB / Nginx)"]

    subgraph AppTier["Stateless Spring Boot Cluster (HPA)"]
        Pod1["Instance 1"]
        Pod2["Instance 2"]
        Pod3["Instance N"]
    end

    ALB --> Pod1
    ALB --> Pod2
    ALB --> Pod3

    subgraph DataTier["Data & Cache Tier"]
        RedisCluster[("Redis Cluster<br/>(Cache-Aside & Session)")]
        DBMaster[("PostgreSQL Primary<br/>(Writes)")]
        DBReplica1[("PostgreSQL Read Replica 1<br/>(Reads)")]
        DBReplica2[("PostgreSQL Read Replica 2<br/>(Reads)")]
    end

    Pod1 & Pod2 & Pod3 -.->|Cache Lookup| RedisCluster
    Pod1 & Pod2 & Pod3 -->|Write Mutations| DBMaster
    Pod1 & Pod2 & Pod3 -->|Read Queries| DBReplica1
    Pod1 & Pod2 & Pod3 -->|Read Queries| DBReplica2

    DBMaster -->|Streaming Replication| DBReplica1
    DBMaster -->|Streaming Replication| DBReplica2

    subgraph EventTier["Asynchronous Processing"]
        KafkaCluster["Kafka Cluster (6+ Partitions)"]
        WorkerPool["Notification & Worker Pods"]
    end

    Pod1 & Pod2 & Pod3 -->|Publish Events| KafkaCluster
    KafkaCluster --> WorkerPool
```

---

## 2. Stateless Backend Scaling (Application Tier)

- **Statelessness:** The backend stores zero user session state in memory. All authentication relies on cryptographically verifiable JWTs.
- **Horizontal Scaling:** Spring Boot instances can scale from 2 to 50+ pods behind a Round-Robin / Least-Connections Load Balancer with zero session stickiness required.
- **Graceful Shutdown:** Configured with `server.shutdown=graceful` so that in-flight requests complete before pod termination during auto-scaling events.

---

## 3. Database Scaling & Optimization

### Connection Pooling (HikariCP)
- The application uses HikariCP with optimized pool limits:
  ```properties
  spring.datasource.hikari.maximum-pool-size=10
  spring.datasource.hikari.minimum-idle=5
  spring.datasource.hikari.connection-timeout=20000
  ```
- **Connection Calculation:** Following PostgreSQL guidelines:
  $$\text{Pool Size} = (\text{CPU Cores} \times 2) + \text{Disk Spindle Count}$$
  Preventing excessive thread context switching at the database engine.

### Read/Write Split (Replication)
- **Primary Node:** Handles all mutations (`INSERT`, `UPDATE`, `DELETE`) with full ACID isolation.
- **Read Replicas:** Read-heavy queries (listing complaints, filtering, search) are routed to asynchronous read replicas using a custom routing `AbstractRoutingDataSource`.

---

## 4. Caching Patterns with Redis

### Cache-Aside Pattern
1. Service checks Redis for requested key.
2. If **Cache Hit**: Returns data directly from memory (< 2ms latency).
3. If **Cache Miss**: Reads from PostgreSQL, saves to Redis with TTL, and returns to user.

```
Read Request ──> Redis Hit? ──Yes──> Return Data
                      │
                     No
                      ▼
               Read PostgreSQL ──> Write to Redis (TTL) ──> Return Data
```

### Cache Invalidation Strategy
- **Explicit Eviction (`@CacheEvict`):** When an admin creates or deletes a category, or when a complaint status changes, the corresponding cache entries are immediately invalidated.
- **Time-to-Live (TTL):** Categories TTL = 1 Hour; Dashboard statistics TTL = 2 Minutes, preventing stale data buildup without manual intervention.

---

## 5. Kafka Streaming Throughput

- **Partitioning:** High-volume topics (e.g. `complaint-events`) are divided across multiple partitions (e.g., 6 or 12 partitions).
- **Parallel Consumer Scaling:** Multiple notification service instances can join the same `consumer-group` (`complaint-notification-group`). Kafka automatically assigns partitions evenly across active consumer pods, achieving horizontal message processing parallelism.
