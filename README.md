# GitPulse

GitPulse is a GitHub Repository Activity Intelligence platform that analyzes repository history to understand code evolution, hotspots, contributor activity, and engineering-risk indicators.

> **Note**: GitPulse is being developed incrementally. This repository contains the domain models, Flyway migrations, and REST APIs for **Repositories** and **Analysis Jobs** (Step 2). Asynchronous job execution (Kafka) and external GitHub ingestion will be connected in future milestones.

---

## Technology Stack

- **Runtime & Language**: Java 21
- **Framework**: Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Spring Validation, Spring Boot Actuator)
- **Database & Migration**: PostgreSQL 16, Flyway Migration (`V1__create_repository_and_analysis_job_tables.sql`)
- **Connection Pool**: HikariCP
- **Build Tool**: Maven (with Maven Wrapper `./mvnw`)
- **Infrastructure**: Docker & Docker Compose (PostgreSQL 16 Alpine)
- **Testing**: Spring Boot Test, MockMvc, JUnit 5, Mockito, H2 (in-memory test mode)

---

## Domain Architecture

GitPulse is structured into focused, domain-oriented modules:

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
└── domain
    ├── analysis
    │   ├── dto
    │   │   └── AnalysisJobResponse.java
    │   ├── AnalysisJob.java
    │   ├── AnalysisJobController.java
    │   ├── AnalysisJobJpaRepository.java
    │   ├── AnalysisJobService.java
    │   └── AnalysisJobStatus.java (PENDING, RUNNING, COMPLETED, FAILED)
    ├── repository
    │   ├── dto
    │   │   ├── CreateRepositoryRequest.java
    │   │   └── RepositoryResponse.java
    │   ├── Repository.java
    │   ├── RepositoryController.java
    │   ├── RepositoryJpaRepository.java
    │   └── RepositoryService.java
    ├── analytics
    ├── commit
    └── contributor
```

---

## Database Schema (Flyway V1)

### `repositories`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PRIMARY KEY, IDENTITY | Unique repository identifier |
| `owner` | VARCHAR(100) | NOT NULL | GitHub repository owner/org |
| `name` | VARCHAR(100) | NOT NULL | Repository name |
| `full_name` | VARCHAR(200) | NOT NULL, UNIQUE | `owner/name` unique identity |
| `description` | VARCHAR(500) | NULL | Repository summary |
| `default_branch` | VARCHAR(100) | NOT NULL, DEFAULT 'main' | Primary git branch |
| `github_id` | BIGINT | NULL | Remote GitHub repository ID |
| `created_at` | TIMESTAMPTZ | NOT NULL | Timestamp of registration |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Timestamp of last update |

*Indexes*: `idx_repositories_full_name`, `idx_repositories_owner_name`

### `analysis_jobs`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PRIMARY KEY, IDENTITY | Unique analysis job identifier |
| `repository_id` | BIGINT | NOT NULL, FK -> repositories(id) | Target repository |
| `status` | VARCHAR(30) | NOT NULL | Lifecycle state (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`) |
| `started_at` | TIMESTAMPTZ | NULL | Analysis start timestamp |
| `completed_at` | TIMESTAMPTZ | NULL | Analysis completion timestamp |
| `error_message` | VARCHAR(1000) | NULL | Diagnostic failure message |
| `created_at` | TIMESTAMPTZ | NOT NULL | Job creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Job update timestamp |

*Indexes*: `idx_analysis_jobs_repository_id`, `idx_analysis_jobs_status`, `idx_analysis_jobs_created_at`

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
```json
{
  "id": 1,
  "owner": "spring-projects",
  "name": "spring-boot",
  "fullName": "spring-projects/spring-boot",
  "description": "Spring Boot makes it easy to create stand-alone Spring applications.",
  "defaultBranch": "main",
  "githubId": null,
  "createdAt": "2026-09-14T00:53:27.649Z",
  "updatedAt": "2026-09-14T00:53:27.649Z"
}
```

### 2. Get Repository Details
- **Endpoint**: `GET /api/v1/repositories/{id}`
- **Response**: `200 OK` (or `404 Not Found` if missing)

### 3. List All Repositories
- **Endpoint**: `GET /api/v1/repositories`
- **Response**: `200 OK` (JSON array of `RepositoryResponse`)

### 4. Create an Analysis Job
- **Endpoint**: `POST /api/v1/repositories/{repositoryId}/analysis-jobs`
- **Description**: Creates and persists an analysis job in `PENDING` status.
- **Response**: `201 Created`
```json
{
  "id": 1,
  "repositoryId": 1,
  "repositoryFullName": "spring-projects/spring-boot",
  "status": "PENDING",
  "startedAt": null,
  "completedAt": null,
  "errorMessage": null,
  "createdAt": "2026-09-14T00:53:26.770Z",
  "updatedAt": "2026-09-14T00:53:26.770Z"
}
```

### 5. Get Analysis Job Status
- **Endpoint**: `GET /api/v1/analysis-jobs/{jobId}`
- **Response**: `200 OK` (or `404 Not Found` if missing)

### 6. Get Analysis Job History for Repository
- **Endpoint**: `GET /api/v1/repositories/{repositoryId}/analysis-jobs`
- **Response**: `200 OK` (JSON array of `AnalysisJobResponse` ordered by creation time descending)

---

## Local Development & Testing

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.9+ (or use `./mvnw`)

### 1. Start PostgreSQL Database
```bash
docker compose up -d
```

### 2. Run Test Suite
```bash
./mvnw clean test
```

### 3. Build Application JAR
```bash
./mvnw clean package
```

### 4. Run Application
```bash
./mvnw spring-boot:run
```
Or run the packaged JAR:
```bash
java -jar target/gitpulse-0.0.1-SNAPSHOT.jar
```