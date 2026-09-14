# GitPulse

GitPulse is a GitHub Repository Activity Intelligence platform that analyzes repository history to understand code evolution, hotspots, contributor activity, and engineering-risk indicators.

> **Note**: GitPulse is being developed incrementally. This repository contains the backend foundation (Step 1), domain models and Flyway migrations for Repositories and Analysis Jobs (Step 2), GitHub REST API Integration Foundation (Step 3), and the Asynchronous Analysis Job Pipeline with Apache Kafka (Step 4). Asynchronous job dispatching and state processing are fully functional; deep commit/file ingestion, Redis caching, and metric computation belong to future milestones.

---

## Technology Stack

- **Runtime & Language**: Java 21
- **Framework**: Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Spring Validation, Spring Boot Actuator, Spring Kafka)
- **Event Streaming & Message Broker**: Apache Kafka (KRaft mode) via `spring-kafka`
- **HTTP Client**: Spring 6 `RestClient` (Synchronous HTTP Client with configurable timeouts and rate-limit handling)
- **Database & Migration**: PostgreSQL 16, Flyway Migrations (`V1` & `V2`)
- **Connection Pool**: HikariCP
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
│   ├── repository
│   │   ├── dto
│   │   │   ├── CreateRepositoryRequest.java
│   │   │   └── RepositoryResponse.java
│   │   ├── Repository.java
│   │   ├── RepositoryController.java
│   │   ├── RepositoryJpaRepository.java
│   │   └── RepositoryService.java
│   ├── analytics
│   ├── commit
│   └── contributor
└── integration
    └── github
        ├── client
        │   └── GitHubRepositoryClient.java
        ├── config
        │   ├── GitHubClientConfig.java
        │   └── GitHubProperties.java
        ├── dto
        │   └── GitHubRepositoryResponse.java
        └── exception
            ├── GitHubApiException.java (HTTP 502)
            ├── GitHubAuthenticationException.java (HTTP 502)
            ├── GitHubRateLimitExceededException.java (HTTP 429)
            ├── GitHubResourceNotFoundException.java (HTTP 404)
            └── GitHubServerException.java (HTTP 502)
```

---

## Asynchronous Processing Pipeline (Kafka)

### Workflow
1. **Client Request**: `POST /api/v1/repositories/{repositoryId}/analysis-jobs`
2. **Job Persistence**: An `AnalysisJob` entity is created in PostgreSQL with `PENDING` status.
3. **Event Publication**: `AnalysisJobService` invokes `AnalysisJobEventProducer` to publish an `AnalysisJobCreatedEvent` to the Kafka topic `gitpulse.analysis-jobs`.
   - **Partition Key**: `repositoryId.toString()` ensures ordered processing per repository across Kafka partitions.
4. **Immediate Response**: The API responds with `201 Created` returning the `PENDING` job details.
5. **Event Consumption**: `AnalysisJobEventConsumer` consumes the event from the topic with consumer group `gitpulse-analysis-workers`.
6. **Processor Execution**: `RepositoryAnalysisProcessor` handles state transitions:
   - Verifies the job exists and is eligible for processing (`PENDING` state).
   - Idempotently ignores duplicate/already-processed jobs.
   - Transitions state: `PENDING` $\rightarrow$ `RUNNING` (`startedAt = Instant.now()`).
   - Executes repository analysis (stubbed in Step 4 for future commit/file ingestion).
   - Transitions state: `RUNNING` $\rightarrow$ `COMPLETED` (`completedAt = Instant.now()`) or `FAILED` (`errorMessage = ...`).
7. **Resilience & DLT**:
   - Up to 3 retry attempts with exponential backoff (1s initial, 2.0x multiplier, 5s max).
   - Unrecoverable failures are routed to `gitpulse.analysis-jobs.DLT` (Dead Letter Topic).

### Event Schema (`AnalysisJobCreatedEvent`)
```json
{
  "jobId": 1,
  "repositoryId": 42,
  "owner": "spring-projects",
  "repoName": "spring-boot",
  "createdAt": "2026-09-14T12:00:00Z"
}
```

---

## Database Migrations

### Flyway V1: Initial Schema (`V1__create_repository_and_analysis_job_tables.sql`)
- Created `repositories` table with identity PK, `owner`, `name`, `full_name` (unique), `description`, `default_branch`, and audit timestamps.
- Created `analysis_jobs` table with `repository_id` foreign key (`ON DELETE CASCADE`), `status`, timestamps, and error message.

### Flyway V2: GitHub Metadata (`V2__add_github_metadata_to_repositories.sql`)
- Enriched `repositories` table with:
  - `html_url` (`VARCHAR(300)`) — GitHub repository URL
  - `primary_language` (`VARCHAR(100)`) — primary programming language
  - `is_private` (`BOOLEAN DEFAULT FALSE`) — repository visibility
  - `pushed_at` (`TIMESTAMPTZ`) — last push timestamp
  - `stars_count` (`INTEGER DEFAULT 0`) — stargazer count
  - `forks_count` (`INTEGER DEFAULT 0`) — fork count
  - `open_issues_count` (`INTEGER DEFAULT 0`) — open issue count

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
| `KAFKA_TOPIC_ANALYSIS_JOBS` | Kafka topic for analysis job creation events | `gitpulse.analysis-jobs` |
| `KAFKA_TOPIC_ANALYSIS_JOBS_DLT` | Kafka topic for dead-letter analysis jobs | `gitpulse.analysis-jobs.DLT` |
| `KAFKA_CONSUMER_GROUP_ID` | Kafka consumer group ID for analysis workers | `gitpulse-analysis-workers` |
| `GITHUB_TOKEN` | Optional GitHub Personal Access Token (for higher rate limits & private repos) | *(empty)* |
| `GITHUB_API_BASE_URL` | Base URL for GitHub REST API | `https://api.github.com` |
| `GITHUB_CONNECT_TIMEOUT` | HTTP connection timeout | `5s` |
| `GITHUB_READ_TIMEOUT` | HTTP response read timeout | `10s` |

---

## REST API Specification

### 1. Register a Repository
- **Endpoint**: `POST /api/v1/repositories`
- **Request Body**:
```json
{
  "owner": "spring-projects",
  "name": "spring-boot",
  "description": "Spring Boot makes it easy to create stand-alone Spring applications.",
  "defaultBranch": "main"
}
```
- **Response**: `201 Created`

### 2. Synchronize Repository Metadata with GitHub
- **Endpoint**: `POST /api/v1/repositories/{id}/sync`
- **Description**: Synchronously queries the GitHub REST API (`GET /repos/{owner}/{repo}`), updates local repository fields (`githubId`, `htmlUrl`, `primaryLanguage`, `starsCount`, `forksCount`, `openIssuesCount`, `pushedAt`), and returns the updated entity.
- **Response**: `200 OK`
```json
{
  "id": 1,
  "owner": "spring-projects",
  "name": "spring-boot",
  "fullName": "spring-projects/spring-boot",
  "description": "Spring Boot makes it easy to create stand-alone Spring applications.",
  "defaultBranch": "main",
  "githubId": 10270250,
  "htmlUrl": "https://github.com/spring-projects/spring-boot",
  "primaryLanguage": "Java",
  "isPrivate": false,
  "pushedAt": "2026-09-13T18:40:00Z",
  "starsCount": 73200,
  "forksCount": 41500,
  "openIssuesCount": 420,
  "createdAt": "2026-09-14T00:53:27.649Z",
  "updatedAt": "2026-09-14T01:05:00.000Z"
}
```

### 3. Get Repository Details
- **Endpoint**: `GET /api/v1/repositories/{id}`
- **Response**: `200 OK` (or `404 Not Found`)

### 4. List All Repositories
- **Endpoint**: `GET /api/v1/repositories`
- **Response**: `200 OK`

### 5. Create an Analysis Job (Asynchronous Dispatch)
- **Endpoint**: `POST /api/v1/repositories/{repositoryId}/analysis-jobs`
- **Description**: Creates and persists an analysis job in `PENDING` state, publishes an `AnalysisJobCreatedEvent` to Kafka, and immediately returns the created job.
- **Response**: `201 Created`
```json
{
  "id": 1,
  "repositoryId": 42,
  "status": "PENDING",
  "createdAt": "2026-09-14T12:00:00Z",
  "startedAt": null,
  "completedAt": null,
  "errorMessage": null
}
```

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