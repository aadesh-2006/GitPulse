# GitPulse

GitPulse is a GitHub Repository Activity Intelligence platform that analyzes repository history to understand code evolution, hotspots, contributor activity, and engineering-risk indicators.

> **Note**: GitPulse is being developed incrementally. This repository contains the backend foundation (Step 1), domain models and Flyway migrations for Repositories and Analysis Jobs (Step 2), GitHub REST API Integration Foundation (Step 3), Asynchronous Analysis Job Pipeline with Apache Kafka (Step 4), GitHub Commit Ingestion & Pagination (Step 5), GitHub File-Change Ingestion (Step 6), Contributor Domain & Activity Attribution (Step 7), File-Level Activity & Churn Aggregation (Step 8), and Deterministic Commit Classification & Intelligence (Step 9). Asynchronous commit history ingestion, deterministic commit classification, commit-detail file-change tracking, materialized contributor attributions, repository file churn read models, and REST query endpoints are fully implemented; code risk scoring and Redis caching belong to future milestones.

---

## Technology Stack

- **Runtime & Language**: Java 21
- **Framework**: Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Spring Validation, Spring Boot Actuator, Spring Kafka)
- **Event Streaming & Message Broker**: Apache Kafka (KRaft mode) via `spring-kafka`
- **HTTP Client**: Spring 6 `RestClient` (Synchronous HTTP Client with configurable timeouts, rate-limit handling, Link header pagination, and commit-detail inspection)
- **Database & Migration**: PostgreSQL 16, Flyway Migrations (`V1`, `V2`, `V3`, `V4`, `V5`, `V6`, `V7`)
- **Connection Pool**: HikariCP (with Hibernate JDBC Batching)
- **Build Tool**: Maven (with Maven Wrapper `./mvnw`)
- **Infrastructure**: Docker & Docker Compose (PostgreSQL 16 Alpine, Apache Kafka 3.8.0 KRaft)
- **Testing**: Spring Boot Test, Embedded Kafka (`@EmbeddedKafka`), MockMvc, MockRestServiceServer, JUnit 5, Mockito, H2 (isolated in-memory test mode)

---

## Architecture & Package Structure

```text
com.gitpulse
├── GitPulseApplication.java
├── common
│   └── exception
│       ├── AppException.java
│       ├── DuplicateResourceException.java (HTTP 409)
│       ├── ErrorResponse.java
│       ├── GlobalExceptionHandler.java
│       └── ResourceNotFoundException.java (HTTP 404)
├── domain
│   ├── analysis
│   │   ├── config
│   │   │   ├── KafkaConsumerConfig.java
│   │   │   └── KafkaTopicConfig.java
│   │   ├── consumer
│   │   │   └── AnalysisJobEventConsumer.java
│   │   ├── dto
│   │   │   └── AnalysisJobResponse.java
│   │   ├── event
│   │   │   └── AnalysisJobCreatedEvent.java
│   │   ├── processor
│   │   │   └── RepositoryAnalysisProcessor.java
│   │   ├── producer
│   │   │   └── AnalysisJobEventProducer.java
│   │   ├── AnalysisJob.java
│   │   ├── AnalysisJobController.java
│   │   ├── AnalysisJobJpaRepository.java
│   │   ├── AnalysisJobService.java
│   │   └── AnalysisJobStatus.java (PENDING, RUNNING, COMPLETED, FAILED)
│   ├── analytics
│   ├── commit
│   │   ├── dto
│   │   │   ├── CommitClassificationResult.java
│   │   │   ├── CommitDetailResponse.java
│   │   │   ├── CommitFileChangeResponse.java
│   │   │   ├── CommitIngestionResult.java
│   │   │   └── CommitResponse.java
│   │   ├── Commit.java
│   │   ├── CommitClassification.java
│   │   ├── CommitClassificationPipelineService.java
│   │   ├── CommitClassificationService.java
│   │   ├── CommitController.java
│   │   ├── CommitJpaRepository.java
│   │   ├── CommitQueryService.java
│   │   ├── CommitSortValidator.java
│   │   └── CommitIngestionService.java

│   ├── contributor
│   │   ├── dto
│   │   │   ├── ContributorAggregationResult.java
│   │   │   ├── ContributorAggregationRow.java
│   │   │   ├── ContributorResponse.java
│   │   │   └── RepositoryContributorResponse.java
│   │   ├── Contributor.java
│   │   ├── ContributorController.java
│   │   ├── ContributorJpaRepository.java
│   │   ├── ContributorService.java
│   │   ├── ContributorAggregationService.java
│   │   └── RepositoryContributorJpaRepository.java
│   ├── file
│   │   ├── dto
│   │   │   ├── FilePrimaryContributorRow.java
│   │   │   ├── PrimaryContributorSummaryResponse.java
│   │   │   ├── RepositoryFileAggregationResult.java
│   │   │   ├── RepositoryFileAggregationRow.java
│   │   │   └── RepositoryFileResponse.java
│   │   ├── FilePathParser.java
│   │   ├── RepositoryFile.java
│   │   ├── RepositoryFileAggregationService.java
│   │   ├── RepositoryFileController.java
│   │   ├── RepositoryFileJpaRepository.java
│   │   ├── RepositoryFileQueryService.java
│   │   └── RepositoryFileSortValidator.java
│   ├── filechange
│   │   ├── dto
│   │   │   └── FileChangeIngestionResult.java
│   │   ├── FileChange.java
│   │   ├── FileChangeJpaRepository.java
│   │   ├── FileChangeIngestionService.java
│   │   └── FileChangeStatus.java (ADDED, MODIFIED, REMOVED, RENAMED, UNKNOWN)
│   └── repository
│       ├── dto
│       │   ├── CreateRepositoryRequest.java
│       │   └── RepositoryResponse.java
│       ├── Repository.java
│       ├── RepositoryController.java
│       ├── RepositoryJpaRepository.java
│       └── RepositoryService.java
└── integration
    └── github
        ├── client
        │   ├── GitHubCommitClient.java
        │   ├── GitHubCommitDetailsClient.java
        │   └── GitHubRepositoryClient.java
        ├── config
        │   ├── GitHubClientConfig.java
        │   └── GitHubProperties.java
        ├── dto
        │   ├── GitHubCommitDetailResponse.java
        │   ├── GitHubCommitPageResponse.java
        │   ├── GitHubCommitResponse.java
        │   ├── GitHubFileResponse.java
        │   └── GitHubRepositoryResponse.java
        └── exception
            ├── GitHubApiException.java (HTTP 502)
            ├── GitHubAuthenticationException.java (HTTP 502)
            ├── GitHubRateLimitExceededException.java (HTTP 429)
            ├── GitHubResourceNotFoundException.java (HTTP 404)
            └── GitHubServerException.java (HTTP 502)
```

---

## Asynchronous Ingestion Pipeline

### Lifecycle & Flow
```text
Client POST /api/v1/repositories/{id}/analysis-jobs
  │
  ▼
AnalysisJob (PENDING) ───[ Kafka Event ]───► AnalysisJobEventConsumer
                                                    │
                                                    ▼
                                       RepositoryAnalysisProcessor
                                                    │
                                                    ▼ (RUNNING)
                                           CommitIngestionService (Stage 1)
                                                    │
                                                    ▼ (Ingest Commits)
                                      CommitClassificationPipelineService (Stage 2)
                                                    │
                                                    ▼ (Classify Commits Deterministically)
                                         FileChangeIngestionService (Stage 3)
                                                    │
                                                    ▼ (Persist File Changes)
                                       ContributorAggregationService (Stage 4)
                                                    │
                                                    ▼ (Aggregate & Persist Contributors)
                                      RepositoryFileAggregationService (Stage 5)
                                                    │
                                                    ▼ (Aggregate & Materialize File Churn)
                                       AnalysisJob (COMPLETED / FAILED)
```

---

## Database Migrations

### Flyway V1: Initial Schema (`V1__create_repository_and_analysis_job_tables.sql`)
- Created `repositories` and `analysis_jobs` tables.

### Flyway V2: GitHub Metadata (`V2__add_github_metadata_to_repositories.sql`)
- Enriched `repositories` with stars, forks, language, privacy, and GitHub ID.

### Flyway V3: Commits Schema (`V3__create_commits_table.sql`)
- Created `commits` table with `uq_commits_repo_sha` and indexing on `(repository_id, committed_at)`.

### Flyway V4: File Changes Schema (`V4__create_file_changes_table.sql`)
- Created `file_changes` table with `uq_file_changes_commit_file` and indexing on `commit_id` and `file_path`.

### Flyway V5: Contributors & Attributions Schema (`V5__create_contributors_and_attributions_tables.sql`)
- Created `contributors` and `repository_contributors` tables.

### Flyway V6: Repository Files Schema (`V6__create_repository_files_table.sql`)
- Created `repository_files` table with churn metrics and primary contributor attributions.

### Flyway V7: Commit Classification (`V7__add_commit_classification.sql`)
- Added `classification VARCHAR(50)` column to `commits` table with composite indexing on `(repository_id, classification)`.

---

## Configuration & Environment Variables

| Variable | Description | Default | Profile / Tier |
|---|---|---|---|
| `SERVER_PORT` | Port for Spring Boot server | `8080` | Application |
| `DB_HOST` | PostgreSQL host | `localhost` | Infrastructure |
| `DB_PORT` | PostgreSQL port | `5432` | Infrastructure |
| `DB_NAME` | PostgreSQL database name | `gitpulse` | Infrastructure |
| `DB_USERNAME` | PostgreSQL username | `gitpulse` | Infrastructure (Secret in Prod) |
| `DB_PASSWORD` | PostgreSQL password | `gitpulse` | Infrastructure (Secret in Prod) |
| `REDIS_HOST` | Redis cache host | `localhost` | Infrastructure |
| `REDIS_PORT` | Redis cache port | `6379` | Infrastructure |
| `EVOLUTION_CACHE_TTL` | Redis cache time-to-live | `5m` | Application Cache |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker bootstrap list | `localhost:9092` | Infrastructure |
| `KAFKA_PRODUCER_MAX_BLOCK_MS` | Kafka producer timeout when buffering | `5000` | Infrastructure |
| `KAFKA_TOPIC_ANALYSIS_JOBS` | Kafka topic for analysis job creation events | `analysis-jobs` | Messaging |
| `KAFKA_TOPIC_ANALYSIS_JOBS_DLT` | Kafka topic for dead-letter analysis jobs | `analysis-jobs.DLT` | Messaging |
| `KAFKA_CONSUMER_GROUP` | Kafka consumer group ID for analysis workers | `gitpulse-analysis-group` | Messaging |
| `KAFKA_CONSUMER_CONCURRENCY` | Kafka consumer concurrency | `1` | Messaging |
| `GITHUB_TOKEN` | Optional GitHub Personal Access Token | *(empty)* | External API (Secret) |
| `GITHUB_API_BASE_URL` | Base URL for GitHub REST API | `https://api.github.com` | External API |
| `GITHUB_CONNECT_TIMEOUT` | HTTP connection timeout | `5s` | External API |
| `GITHUB_READ_TIMEOUT` | HTTP response read timeout | `10s` | External API |
| `GITHUB_COMMIT_PAGE_SIZE` | Commits per page from GitHub API (1–100) | `30` | External API |
| `VITE_API_PROXY_TARGET` | Frontend Vite dev server proxy target | `http://localhost:8080` | Frontend Dev |
| `VITE_API_BASE_URL` | Frontend API client base URL | `/api/v1` | Frontend Client |

---

## REST API Specification

### 1. Register a Repository
- **Endpoint**: `POST /api/v1/repositories`
- **Response**: `201 Created`

### 2. Synchronize Repository Metadata with GitHub
- **Endpoint**: `POST /api/v1/repositories/{id}/sync`
- **Response**: `200 OK`

### 3. Get Repository Details
- **Endpoint**: `GET /api/v1/repositories/{id}`
- **Response**: `200 OK` (or `404 Not Found`)

### 4. List All Repositories
- **Endpoint**: `GET /api/v1/repositories`
- **Response**: `200 OK`

### 5. Create an Analysis Job (Asynchronous Ingestion & Attribution)
- **Endpoint**: `POST /api/v1/repositories/{repositoryId}/analysis-jobs`
- **Description**: Creates an analysis job in `PENDING` state, publishes an event to Kafka, and initiates background commit ingestion, deterministic commit classification, file-change tracking, contributor aggregation, and file churn materialization.
- **Response**: `201 Created`

### 6. Get Analysis Job Status
- **Endpoint**: `GET /api/v1/analysis-jobs/{jobId}`
- **Response**: `200 OK` (or `404 Not Found`)

### 7. Get Repository Analysis History
- **Endpoint**: `GET /api/v1/repositories/{repositoryId}/analysis-jobs`
- **Response**: `200 OK`

### 8. List Repository Contributors (Paginated & Sorted)
- **Endpoint**: `GET /api/v1/repositories/{repositoryId}/contributors?page=0&size=20&sort=totalCommits,desc`
- **Response**: `200 OK` (Spring Data `Page<RepositoryContributorResponse>`)

### 9. Get Contributor Details
- **Endpoint**: `GET /api/v1/contributors/{contributorId}`
- **Response**: `200 OK` (or `404 Not Found`)

### 10. Get Specific Repository Contributor Attribution
- **Endpoint**: `GET /api/v1/repositories/{repositoryId}/contributors/{contributorId}`
- **Response**: `200 OK` (or `404 Not Found`)

### 11. List Repository Files (Paginated, Sorted & Filtered)
- **Endpoint**: `GET /api/v1/repositories/{repositoryId}/files?page=0&size=20&sort=totalChurn,desc&extension=java&isDeleted=false`
- **Supported Query Parameters**: `page`, `size`, `sort`, `extension`, `isDeleted`
- **Allowed Sort Fields**: `filePath`, `totalRevisions`, `totalAdditions`, `totalDeletions`, `totalChurn`, `firstModifiedAt`, `lastModifiedAt`
- **Default Sort**: `totalChurn,desc`
- **Response**: `200 OK` (Spring Data `Page<RepositoryFileResponse>`)

### 12. Query Repository Hotspots
- **Endpoint**: `GET /api/v1/repositories/{repositoryId}/files/hotspots?page=0&size=20&sort=totalChurn,desc`
- **Supported Query Parameters**: `page`, `size`, `sort`, `extension`, `isDeleted`
- **Default Sort**: `totalChurn,desc`
- **Response**: `200 OK` (Spring Data `Page<RepositoryFileResponse>`)

### 13. Get Repository File Intelligence Detail
- **Endpoint**: `GET /api/v1/repositories/{repositoryId}/files/{*filePath}`
- **Example**: `GET /api/v1/repositories/1/files/src/main/java/com/gitpulse/GitPulseApplication.java`
- **Response**: `200 OK` (`RepositoryFileResponse` with primary contributor details) or `404 Not Found`

### 14. List Repository Commits (Paginated, Sorted & Filtered)
- **Endpoint**: `GET /api/v1/repositories/{repositoryId}/commits`
- **Supported Query Parameters**:
  - `page` (default `0`)
  - `size` (default `20`)
  - `sort` (default `committedAt,desc`)
  - `classification` (optional `CommitClassification`: `FEATURE`, `BUG_FIX`, `REFACTOR`, `DOCUMENTATION`, `TEST`, `BUILD`, `CONFIGURATION`, `DEPENDENCY`, `OTHER`)
  - `authorEmail` (optional `String`, case-insensitive match)
  - `from` (optional `ISO-8601 Instant`, e.g. `2026-09-01T00:00:00Z`)
  - `to` (optional `ISO-8601 Instant`, e.g. `2026-09-15T23:59:59Z`)
- **Allowed Sort Fields**: `committedAt`, `additions`, `deletions`, `totalChanges`, `githubCommitSha`, `id`
- **Default Sort**: `committedAt,desc`
- **Response**: `200 OK` (Spring Data `Page<CommitResponse>`)

### 15. Get Commit Intelligence Detail
- **Endpoint**: `GET /api/v1/repositories/{repositoryId}/commits/{commitId}`
- **Example**: `GET /api/v1/repositories/1/commits/42`
- **Response**: `200 OK` (`CommitDetailResponse` with commit metadata and associated `List<CommitFileChangeResponse>`) or `404 Not Found`

---

## Observability & Health Endpoints

GitPulse exposes production-ready operational telemetry and probe endpoints via Spring Boot Actuator:

| Endpoint | Description | Expected Status / Value |
|---|---|---|
| `GET /actuator/health` | Overall aggregate health status | `{"status":"UP"}` |
| `GET /actuator/health/liveness` | Kubernetes/container liveness probe | `{"status":"UP"}` |
| `GET /actuator/health/readiness` | Readiness probe (checks DB, disk space) | `{"status":"UP"}` (503 if DB down) |
| `GET /actuator/info` | Application metadata | `{"app":{"name":"gitpulse"}}` |
| `GET /actuator/metrics` | Available Micrometer metric keys | List of metrics |
| `GET /actuator/metrics/{name}` | Metric telemetry (e.g. `gitpulse.analysis.jobs`) | Value and tags |

---

## Local Development & Testing

### Prerequisites
- **Java**: 21+
- **Node.js**: 18+ (with npm)
- **Docker & Docker Compose**: for PostgreSQL 16, Kafka KRaft, and Redis 7
- **Maven**: 3.9+ (or use `./mvnw` / `.\mvnw.cmd`)

### 1. Start Infrastructure (PostgreSQL, Apache Kafka & Redis)
```bash
docker compose up -d
```

### 2. Run Backend Test Suite (Isolated In-Memory Mode)
```bash
# Unix/macOS
./mvnw clean test

# Windows
.\mvnw.cmd clean test
```

### 3. Run Backend Application
```bash
# Unix/macOS
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

### 4. Run Frontend Application (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
The frontend will start at `http://localhost:3000` and proxy API calls to `http://localhost:8080`.

### 5. Build Frontend for Production
```bash
cd frontend
npm run build
```

### 6. Stop Infrastructure
```bash
docker compose down
```

### 7. Reset Development Database
To purge persistent volumes and reset the development database cleanly:
```bash
docker compose down -v
```