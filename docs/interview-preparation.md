# Software Engineer Interview Preparation & Technical Defense Guide

This guide contains in-depth technical questions, tradeoffs, and battle-tested answers covering every architectural decision made in this project. Use this to defend your engineering choices during technical and system design interviews.

---

## 1. System Architecture & Modular Monolith vs. Microservices

### Q: Why did you build this as a modular monolith instead of starting with microservices?
**Answer:**
"Starting with microservices for an early-stage application introduces heavy operational complexity without delivering proportional value. Microservices bring distributed transaction challenges (requiring Saga patterns or two-phase commits), network latency on inter-service hops, data consistency issues, and complex distributed tracing.

Instead, I designed a **modular monolith** with clean, decoupled domain packages (`auth`, `complaints`, `comments`, `notifications`). All module communication is bounded by service interfaces and asynchronous Kafka event publishers. This gives us the velocity and simplicity of a single deployable artifact while ensuring that when traffic or team organization demands extraction, each module can be carved into an independent microservice with zero changes to business domain logic."

---

## 2. Spring Boot Core

### Q: What is Spring Boot's Auto-Configuration and how does `@SpringBootApplication` work?
**Answer:**
"`@SpringBootApplication` is a meta-annotation composed of three core annotations:
1. `@SpringBootConfiguration`: Marks the class as a configuration source for Spring beans.
2. `@EnableAutoConfiguration`: Instructs Spring Boot to scan classpath dependencies (e.g. `spring-boot-starter-data-jpa`, `postgresql`) and automatically configure sensible defaults (such as DataSource, EntityManagerFactory, and TransactionManager) defined in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
3. `@ComponentScan`: Scans for components (`@Service`, `@Repository`, `@Controller`, `@Component`) in the current package and sub-packages.

I customized auto-configuration by configuring conditional beans, such as `@ConditionalOnProperty` for Redis caching and explicit security filter chains."

---

## 3. REST API Design & HTTP Standards

### Q: Why do you use DTOs instead of returning JPA entities directly from controllers?
**Answer:**
"Exposing JPA entities directly across API boundaries creates two major vulnerabilities:
1. **Mass Assignment Vulnerability (Over-posting):** If an entity is accepted in `@RequestBody`, a malicious user can inject unexpected JSON fields (such as `role = ROLE_ADMIN` or `status = RESOLVED`) that overwrite database columns during binding.
2. **Infinite Recursion & Lazy Initialization Failures:** Bidirectional JPA relationships (e.g. `@OneToMany` complaints to comments) can trigger `JsonMappingException` circular references or `LazyInitializationException` when serialized outside an active transaction.

DTOs decouple the external API contract from internal database schemas, enforce strict field validation (`@NotBlank`, `@Size`), and allow API versioning without database schema migrations."

### Q: What is the difference between PUT and PATCH?
**Answer:**
"`PUT` is an idempotent replacement of the entire resource state. If fields are omitted in a PUT payload, they should be cleared or set to defaults. `PATCH` applies partial modifications to a resource (e.g. updating only the `status` or assigning an `agentId` without sending the entire complaint description)."

---

## 4. JWT & Authentication

### Q: How does JWT authentication work and how do you handle logout or revocation?
**Answer:**
"JWT is a stateless token containing a header, payload (claims), and signature.
1. When a user authenticates (`POST /api/auth/login`), the server verifies the BCrypt-hashed password and issues a signed token (`HS256`).
2. Subsequent requests attach this token via the `Authorization: Bearer <token>` header.
3. The `JwtAuthenticationFilter` validates the signature using our symmetric HMAC secret, checks expiration (`exp`), extracts the subject and roles, and loads an authenticated principal into `SecurityContextHolder`.

**Revocation Strategy:** Since JWTs are stateless, they remain valid until expiration. In production, revocation is handled via:
- Short token expiration lifetimes (e.g. 15 minutes) paired with Refresh Tokens.
- A **Redis Token Blacklist**: On logout, the token's remaining TTL is stored in Redis; the filter checks Redis before accepting the token."

---

## 5. PostgreSQL & Database Relational Modeling

### Q: Why choose PostgreSQL over MySQL or MongoDB?
**Answer:**
"Complaints inherently require strong relational guarantees: complaints belong to categories and citizens, have multiple audit trail entries, and are assigned to specific agents. PostgreSQL provides:
- **Strict ACID Transactions:** Critical for status transitions and assignment integrity.
- **Advanced Concurrency Control:** Multi-Version Concurrency Control (MVCC) allows readers not to block writers and writers not to block readers.
- **Rich Indexing:** B-Tree indexing on status, priority, and foreign keys ensures fast filtering across millions of records.
- **Extensibility:** Native JSONB support if semi-structured attachments or survey answers are added in the future."

---

## 6. JPA / Hibernate & ORM Gotchas

### Q: What is the N+1 Query Problem in JPA and how did you prevent it?
**Answer:**
"The N+1 problem occurs when fetching an entity with lazy relationships (like 100 complaints having an associated `Category` or `User`). The ORM executes 1 query to fetch the complaints, followed by N separate SQL queries to fetch each complaint's category.

**Mitigations implemented:**
1. Using DTO projections so only required fields are queried.
2. Using `JOIN FETCH` queries or Spring Data JPA `@EntityGraph` for eager retrieval in a single SQL join when full child graphs are required.
3. Modeling relationships with `FetchType.LAZY` by default to avoid accidental graph loading."

---

## 7. Spring Transactions (`@Transactional`)

### Q: What happens when an exception occurs inside a `@Transactional` method?
**Answer:**
"By default in Spring, transactions roll back automatically only for **unchecked exceptions** (`RuntimeException` and `Error`). Checked exceptions (`Exception`) do **not** trigger a rollback unless explicitly configured with `@Transactional(rollbackFor = Exception.class)`.

In our service layer:
- Read-only operations use `@Transactional(readOnly = true)` which optimizes database resource usage, disables Hibernate dirty-checking, and can route queries to read replicas.
- Write operations run in write transactions ensuring atomic updates across `complaints` and `complaint_history` tables."

---

## 8. Apache Kafka & Event-Driven Architecture

### Q: Why use Kafka instead of standard RabbitMQ or an in-memory queue?
**Answer:**
"Kafka is a distributed, append-only commit log with durable storage and partition-based consumer scaling.
1. **Durable Storage & Replayability:** Unlike RabbitMQ where messages are discarded upon acknowledgement, Kafka persists messages. If downstream notification workers crash or need to re-index data, consumers can replay events by resetting their consumer offset.
2. **High Throughput:** Kafka's sequential disk I/O, OS page-cache usage, and zero-copy data transfer (`sendfile`) yield throughput orders of magnitude higher than broker-based message queues.
3. **Partition Ordering:** Using `complaintId` as the Kafka message key guarantees that all events for a given complaint are processed in strict chronological order by the same partition consumer."

### Q: How do you handle duplicate events (At-Least-Once Delivery)?
**Answer:**
"Network hiccups and consumer rebalances can cause a consumer to process a message multiple times. We implement **Idempotent Consumers**:
- Each message contains a unique event identifier.
- The consumer records processed event IDs in an idempotent table (`INSERT ... ON CONFLICT DO NOTHING`) before executing business side-effects, ensuring duplicate deliveries are safely ignored."

---

## 9. Docker & Containerization

### Q: Why did you use multi-stage Docker builds?
**Answer:**
"A standard single-stage build includes the Maven build tools, local repository cache, and source files in the final Docker image, resulting in a bloated image (> 800MB) with an enlarged security attack surface.

In our **multi-stage build**:
1. Stage 1 (`builder`) uses `maven:3.9.9-eclipse-temurin-17-alpine` to compile code and build the JAR.
2. Stage 2 (`runtime`) uses a minimal `eclipse-temurin:17-jre-alpine` runtime image, copying only the final 60MB executable JAR.
3. We run the application under a non-root user (`appuser`) rather than `root` to prevent container breakout vulnerabilities."

---

## 10. CI/CD & GitHub Actions

### Q: Describe your CI/CD pipeline stages.
**Answer:**
"Our pipeline triggers on every push and pull request:
1. **Source Checkout:** Retrieves code.
2. **JDK 17 Setup & Dependency Caching:** Uses `actions/setup-java` with Maven cache to drastically accelerate subsequent runs.
3. **Automated Testing:** Runs `mvn clean test` executing all unit tests and MockMvc controller slice tests. If any assertion fails, the pipeline aborts.
4. **Packaging:** Verifies artifact creation with `mvn package`.
5. **Docker Build Verification:** Builds the backend and frontend Docker images to ensure container configuration integrity."

---

## 11. Redis & Performance Caching

### Q: What is the Cache-Aside pattern and how do you handle cache invalidation?
**Answer:**
"In Cache-Aside (Lazy Loading):
- The application first checks Redis. On a hit, it returns the cached data immediately.
- On a miss, it queries PostgreSQL, writes the result to Redis with a TTL, and returns the result.

**Invalidation Strategy:**
- We apply `@CacheEvict(value = 'categories', allEntries = true)` whenever an admin modifies categories.
- Dashboard metrics use a short 2-minute TTL, providing high-performance caching for expensive aggregations while bounding stale data window to 120 seconds."

---

## 12. Scalability & High Availability

### Q: How would you scale this system from 1,000 to 100,000 active users?
**Answer:**
"1. **Stateless Backend Scaling:** Deploy multiple Spring Boot instances behind an Application Load Balancer with Horizontal Pod Autoscaler (HPA) scaling pods based on CPU/Memory/Request metrics.
2. **Database Read/Write Split:** Route read-heavy queries to PostgreSQL read replicas using Spring's `AbstractRoutingDataSource`.
3. **Redis Cluster:** Cache taxonomy data and session metadata to reduce database query pressure.
4. **Kafka Partition Scaling:** Scale Kafka topic partitions and consumer instances to process notifications and alerts asynchronously without impacting user response times."

---

## 13. Unit & Integration Testing

### Q: What is the difference between `@SpringBootTest` and `@WebMvcTest`?
**Answer:**
"`@SpringBootTest` boots the full application context including database repositories, security, and services. It is ideal for end-to-end integration tests but runs slower.

`@WebMvcTest` is a specialized test slice that only boots web layer components (`@Controller`, `@ControllerAdvice`, Jackson converters) and does not load JPA repositories or services. Dependencies are mocked using `@MockBean`. This provides fast, isolated tests for HTTP status codes, JSON serialization, and input validation without needing a database."

---

## 14. Observability & Debugging

### Q: How do you debug a production issue where an API request fails intermittently?
**Answer:**
"1. **Correlation IDs (MDC):** Attach a unique `X-Correlation-ID` header to every incoming HTTP request and inject it into SLF4J's Mapped Diagnostic Context (MDC). All log statements across services will print this ID, allowing distributed log aggregation (e.g. ELK or Datadog) to trace the exact request journey.
2. **Spring Boot Actuator:** Inspect `/actuator/health`, `/actuator/metrics`, and database connection pool saturation in HikariCP.
3. **Structured Logging:** Review error-level logs containing full exception stack traces and contextual parameter data."

---

---

## 15. Failure Handling & System Resilience

### Q: How does the system handle infrastructure failures (Kafka down, Redis down, Database connection drop)?
**Answer:**
"In production systems, dependencies inevitably fail. The application is architected for graceful degradation:
1. **Kafka Failure Resiliency:**
   - The `ComplaintEventPublisher` wraps Kafka publishing in asynchronous callbacks with logging and exception interception.
   - If Kafka is unreachable or partition brokers reject writes, the core complaint creation transaction does not crash the citizen's request.
   - For enterprise guarantees, we employ the **Transactional Outbox Pattern**: domain events are saved in the same PostgreSQL transaction into an `outbox` table, and an asynchronous de-queuer retries sending events to Kafka until acknowledged, preventing message loss during broker downtime.
2. **Dead Letter Queue (DLQ) for Poison Pill Events:**
   - When a consumer encounters unprocessable or corrupt event payloads, after a fixed number of retries with exponential backoff, the event is routed to `complaint-events.DLQ` for offline debugging without blocking partition processing.
3. **Redis Cache Failure Graceful Degradation:**
   - Redis operates purely as a performance accelerator, not a source of truth. If Redis crashes or experiences network timeouts, Spring Cache falls through directly to PostgreSQL. The user experiences slightly higher latency, but zero downtime or functional disruption.
4. **Database Connection Pool Resilience (HikariCP):**
   - Configured with `connection-timeout: 20000ms`, `idle-timeout: 300000ms`, and validation query `isValid()`. If PostgreSQL restarts, HikariCP automatically evicts broken sockets and re-establishes healthy pooled connections transparently."

---

## 16. Soft Deletion, Audit Compliance & Index Optimization

### Q: Why did you implement soft deletion instead of hard DELETE, and how does it impact query performance?
**Answer:**
"In incident management and regulatory environments, hard `DELETE` causes irreversible data loss and destroys legal compliance audit trails.
- **Audit Preservation:** We maintain `is_deleted`, `deleted_at`, `deleted_by` (User foreign key), and `delete_reason`. This ensures administrators know who removed a ticket, when, and why, with the ability to instantly restore it.
- **Foreign Key Integrity:** Hard-deleting complaints causes cascading deletion or orphaned records in `comments` and `complaint_history`. Soft delete preserves relational graph integrity.
- **Index Performance Tradeoff:** To prevent deleted records from degrading active ticket query performance:
  - Active queries filter on `is_deleted = false`.
  - In PostgreSQL, partial indexes (`CREATE INDEX idx_active_complaints ON complaints(status, priority) WHERE is_deleted = false;`) ensure index trees remain compact and fast regardless of how many soft-deleted records accumulate."

---

## 17. Real Engineering vs. Buzzwords

### Q: How did you ensure every technology in this project had a genuine purpose?
**Answer:**
"Every technology was introduced to solve a specific engineering problem:
- **PostgreSQL:** Replaced ephemeral in-memory storage to provide ACID durability and relational integrity for complaint workflows.
- **JWT & BCrypt:** Replaced insecure `permitAll()` endpoints with stateless, cryptographically secure authentication and role-based access.
- **DTOs & Bean Validation:** Eliminated mass-assignment vulnerabilities and standardized client error reporting.
- **Kafka:** Decoupled slow notification dispatch from critical synchronous request paths.
- **Redis:** Offloaded repetitive category lookups and expensive aggregate dashboard calculations.
- **Docker & GitHub Actions:** Standardized cross-platform execution and ensured continuous automated test verification."

