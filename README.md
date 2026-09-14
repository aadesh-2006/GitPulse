# GitPulse

GitPulse is a GitHub Repository Activity Intelligence platform that analyzes repository history to understand code evolution, hotspots, contributor activity, and engineering-risk indicators.

> **Note**: GitPulse is being developed incrementally. This repository contains the backend foundation (Step 1), domain models and Flyway migrations for Repositories and Analysis Jobs (Step 2), GitHub REST API Integration Foundation (Step 3), Asynchronous Analysis Job Pipeline with Apache Kafka (Step 4), GitHub Commit Ingestion & Pagination (Step 5), and GitHub File-Change Ingestion (Step 6). Asynchronous commit history ingestion, commit-detail file-change tracking, and idempotent batch persistence are fully implemented; contributor graph analytics, code hotspot scoring, and Redis caching belong to future milestones.

---

## Technology Stack

- **Runtime & Language**: Java 21
- **Framework**: Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Spring Validation, Spring Boot Actuator, Spring Kafka)
- **Event Streaming & Message Broker**: Apache Kafka (KRaft mode) via `spring-kafka`
- **HTTP Client**: Spring 6 `RestClient` (Synchronous HTTP Client with configurable timeouts, rate-limit handling, Link header pagination, and commit-detail inspection)
- **Database & Migration**: PostgreSQL 16, Flyway Migrations (`V1`, `V2`, `V3`, `V4`)
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
│   ├── commit
│   │   ├── dto
│   │   │   └── CommitIngestionResult.java
│   │   ├── Commit.java
│   │   ├── CommitJpaRepository.java
│   │   └── CommitIngestionService.java
│   ├── filechange
│   │   ├── dto
│   │   │   └── FileChangeIngestionResult.java
│   │   ├── FileChange.java
│   │   ├── FileChangeJpaRepository.java
│   │   ├── FileChangeIngestionService.java
│   │   └── FileChangeStatus.java (ADDED, MODIFIED, REMOVED, RENAMED, UNKNOWN)
│   ├── repository
│   │   ├── dto
│   │   │   ├── CreateRepositoryRequest.java
│   │   │   └── RepositoryResponse.java
│   │   ├── Repository.java
│   │   ├── RepositoryController.java
│   │   ├── RepositoryJpaRepository.java
│   │   └── RepositoryService.java
│   ├── analytics
│   └── contributor
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
                                           CommitIngestionService
                                                    │
                                                    ▼ (Persist Commits)
                                         FileChangeIngestionService
                                                    │
                                                    ▼ (Persist File Changes)
                                       AnalysisJob (COMPLETED / FAILED)
```

### 1. Commit Ingestion & Link Header Pagination
- Commits are fetched in discrete pages using `GET /repos/{owner}/{repo}/commits?page={page}&per_page={pageSize}` starting at `page=1`.
- Pagination is driven solely by the presence of `rel="next"` in the RFC 5988 `Link` response header.
- For each page received, existing commit SHAs are identified via `findExistingGithubCommitShas` and only new commits are persisted.

### 2. File-Change Ingestion & Detail Inspection
- For persisted commits, file metadata is fetched via `GET /repos/{owner}/{repo}/commits/{sha}` using `GitHubCommitDetailsClient`.
- Commits are processed in memory-safe chunks (50 commits per page).
- **N+1 DB Query Prevention**: Before calling GitHub, a single batch query `findCommitIdsWithFileChanges(commitIds)` identifies which commits already have file changes persisted, skipping redundant API requests.
- **Normalization & Mapping**: GitHub file entries are normalized into `FileChange` entities with statuses (`ADDED`, `MODIFIED`, `REMOVED`, `RENAMED`, `UNKNOWN`). For renamed files, the target `filename` is stored as `file_path`.
- **Intra-Commit Deduplication**: Duplicate file paths returned within the same commit detail response are deduplicated in memory.
- **Commit Stats Enrichment**: `additions`, `deletions`, and `total_changes` on the parent `Commit` entity are populated if they were null during list ingestion.

### 3. API Limitations & Edge Cases
- **GitHub 300-File Limit**: GitHub's commit detail endpoint returns up to 300 files per commit. The pipeline persists all files provided in the response without repository cloning.
- **Zero-File Commits**: Commits returning `files: []` (e.g. merge commits without file alterations or empty commits) produce no `FileChange` rows. In Step 6, these commit IDs will not appear in `findCommitIdsWithFileChanges` and may be queried again upon re-analysis.
- **Error Propagation**: Any unrecoverable GitHub error (such as HTTP 403/429 rate limit or 5xx server errors) fails the `AnalysisJob` immediately, recording the `errorReason` on the job rather than producing incomplete analysis data.

### 4. Database Schema & Batch Persistence
- `commits` and `file_changes` tables enforce composite unique constraints (`uq_commits_repo_sha` and `uq_file_changes_commit_file`) guaranteeing storage-layer idempotency.
- File changes are saved using Hibernate JDBC batching (`batch_size: 50`, `order_inserts: true`, `order_updates: true`) in discrete transactions per batch.

---

## Database Migrations

### Flyway V1: Initial Schema (`V1__create_repository_and_analysis_job_tables.sql`)
- Created `repositories` and `analysis_jobs` tables with identity PKs, unique constraints, and foreign keys.

### Flyway V2: GitHub Metadata (`V2__add_github_metadata_to_repositories.sql`)
- Enriched `repositories` table with `html_url`, `primary_language`, `is_private`, `pushed_at`, `stars_count`, `forks_count`, and `open_issues_count`.

### Flyway V3: Commits Schema (`V3__create_commits_table.sql`)
- Created `commits` table:
  - `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`
  - `repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE`
  - `github_commit_sha VARCHAR(40) NOT NULL`
  - `message TEXT NOT NULL`
  - `author_name VARCHAR(200)`
  - `author_email VARCHAR(200)`
  - `author_username VARCHAR(100)`
  - `committed_at TIMESTAMPTZ NOT NULL`
  - `additions INTEGER`, `deletions INTEGER`, `total_changes INTEGER`
  - `html_url VARCHAR(300)`
  - `created_at TIMESTAMPTZ NOT NULL`
  - `CONSTRAINT uq_commits_repo_sha UNIQUE (repository_id, github_commit_sha)`
  - Indexes: `idx_commits_repository_id`, `idx_commits_repo_committed_at`.

### Flyway V4: File Changes Schema (`V4__create_file_changes_table.sql`)
- Created `file_changes` table:
  - `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`
  - `commit_id BIGINT NOT NULL REFERENCES commits(id) ON DELETE CASCADE`
  - `file_path VARCHAR(500) NOT NULL`
  - `status VARCHAR(50) NOT NULL`
  - `additions INTEGER`, `deletions INTEGER`, `changes INTEGER`
  - `blob_url VARCHAR(500)`, `raw_url VARCHAR(500)`
  - `created_at TIMESTAMPTZ NOT NULL`
  - `CONSTRAINT uq_file_changes_commit_file UNIQUE (commit_id, file_path)`
  - Indexes: `idx_file_changes_commit_id`, `idx_file_changes_file_path`.

---

## Configuration & Environment Variables

| Variable | Description | Default |
|---|---|---|
| `SERVER_PORT` | Port for Spring Boot server | `8080` |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | PostgreSQL database name | `gitpulse` |
| `DB_USERNAME` | PostgreSQL username | `gitpulse` |
| `DB_PASSWORD` | PostgreSQL password | `gitpulse` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker bootstrap list | `localhost:9092` |
| `KAFKA_TOPIC_ANALYSIS_JOBS` | Kafka topic for analysis job creation events | `analysis-jobs` |
| `KAFKA_TOPIC_ANALYSIS_JOBS_DLT` | Kafka topic for dead-letter analysis jobs | `analysis-jobs.DLT` |
| `KAFKA_CONSUMER_GROUP` | Kafka consumer group ID for analysis workers | `gitpulse-analysis-group` |
| `KAFKA_CONSUMER_CONCURRENCY` | Kafka consumer concurrency | `1` |
| `GITHUB_TOKEN` | Optional GitHub Personal Access Token | *(empty)* |
| `GITHUB_API_BASE_URL` | Base URL for GitHub REST API | `https://api.github.com` |
| `GITHUB_CONNECT_TIMEOUT` | HTTP connection timeout | `5s` |
| `GITHUB_READ_TIMEOUT` | HTTP response read timeout | `10s` |
| `GITHUB_COMMIT_PAGE_SIZE` | Commits per page from GitHub API (1–100) | `30` |

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

### 5. Create an Analysis Job (Asynchronous Commit Ingestion)
- **Endpoint**: `POST /api/v1/repositories/{repositoryId}/analysis-jobs`
- **Description**: Creates and persists an analysis job in `PENDING` state, publishes an event to Kafka, and initiates background commit history ingestion.
- **Response**: `201 Created`

### 6. Get Analysis Job Status
- **Endpoint**: `GET /api/v1/analysis-jobs/{jobId}`
- **Response**: `200 OK` (or `404 Not Found`)

### 7. Get Repository Analysis History
- **Endpoint**: `GET /api/v1/repositories/{repositoryId}/analysis-jobs`
- **Response**: `200 OK`

---

## Local Development & Testing

### Prerequisites
- Java 21+
- Docker & Docker Compose (for PostgreSQL 16 & Kafka KRaft)
- Maven 3.9+ (or use `./mvnw`)

### 1. Start Infrastructure (PostgreSQL & Apache Kafka)
```bash
docker compose up -d
```

### 2. Run Test Suite (Offline / Embedded Kafka)
```bash
./mvnw clean test
```

### 3. Package Application
```bash
./mvnw clean package
```

### 4. Run Application
```bash
./mvnw spring-boot:run
```