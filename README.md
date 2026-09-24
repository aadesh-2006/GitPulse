# GitPulse

GitPulse is an engineering-intelligence platform for analyzing Git repository history and deriving repository evolution, file hotspots, contributor activity, ownership concentration, commit intelligence, and deterministic risk/stability signals.

> **Project Status**: The core backend analytical services, asynchronous Kafka analysis pipeline, Redis caching layer, and React + Vite analytics dashboards are fully implemented and verified against **533 passing automated tests**. The research benchmark methodology, statistical evaluation engine, and 15-repository benchmark configuration are implemented. The actual live 15-repository benchmark study execution remains **pending** local infrastructure and data bootstrap.

---

## Implementation & Milestone Status

```text
┌────────────────────────────────────────────────────────────────────────┐
│                              IMPLEMENTED                               │
├────────────────────────────────────────────────────────────────────────┤
│  ✓ Core Repository & Analysis Domain                                   │
│  ✓ Asynchronous Kafka Pipeline & Event-Driven Processing               │
│  ✓ GitHub REST API Client (Pagination, Rate-Limits, Commit Details)    │
│  ✓ Commit Ingestion & Deterministic Classification Engine              │
│  ✓ File-Change Tracking & File Hotspot Intelligence                    │
│  ✓ Contributor Attribution & File Ownership Concentration              │
│  ✓ Deterministic Risk & Stability Scoring Model                        │
│  ✓ Monthly Repository Evolution Analytics & Period Comparison          │
│  ✓ Redis Best-Effort Caching with PostgreSQL Fallback                  │
│  ✓ Production Observability, Micrometer Metrics & Actuator Probes      │
│  ✓ API Validation & Security Hardening                                 │
│  ✓ 6-View React + TypeScript + Vite Analytics Dashboard                │
│  ✓ Temporal Benchmark Methodology & Statistical Calculation Engine     │
│  ✓ 15-Repository Benchmark Study Configuration & Cutoff Selector       │
└────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│                          IN PROGRESS / PENDING                         │
├────────────────────────────────────────────────────────────────────────┤
│  ⏳ Local Benchmark Infrastructure Bootstrap (Docker/Kafka/PostgreSQL) │
│  ⏳ Real Ingestion of 15 Benchmark Target Repositories                 │
│  ⏳ Execution of 45 Planned Benchmark Runs (15 Repos × 3 Cutoffs)      │
│  ⏳ Export of Real Benchmark Research CSVs & Statistical Results       │
│  ⏳ Generation of Research Visualizations from Measured Data           │
│  ⏳ Empirical Findings & Research Conclusions                          │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

- **Runtime & Language**: Java 21, TypeScript 5.6
- **Backend Framework**: Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Spring Validation, Spring Boot Actuator, Spring Kafka, Spring Data Redis)
- **Frontend Framework**: React 18, Vite 5, Recharts 3, Tailwind CSS
- **Event Streaming & Message Broker**: Apache Kafka 3.8.0 (KRaft mode) via `spring-kafka`
- **Cache Layer**: Redis 7 Alpine (Lettuce driver with automatic database fallback)
- **HTTP Client**: Spring 6 `RestClient` (Synchronous HTTP Client with configurable timeouts, rate-limit handling, Link header pagination, and commit-detail inspection)
- **Database & Migration**: PostgreSQL 16, Flyway Migrations (`V1` through `V9`)
- **Connection Pool**: HikariCP (with Hibernate JDBC Batching)
- **Build Tool**: Maven (with Maven Wrapper `./mvnw`), npm
- **Infrastructure**: Docker & Docker Compose (PostgreSQL 16 Alpine, Apache Kafka 3.8.0 KRaft, Redis 7 Alpine)
- **Testing**: Spring Boot Test, Embedded Kafka (`@EmbeddedKafka`), MockMvc, MockRestServiceServer, JUnit 5, Mockito, H2 (isolated in-memory test mode)

---

## Architecture & Data Flow

```text
GitHub REST API
      │
      ▼
Repository / Analysis Job REST API
      │
      ▼
Apache Kafka (analysis-jobs topic)
      │
      ▼
AnalysisJobEventConsumer
      │
      ▼
RepositoryAnalysisProcessor (Pipeline Orchestrator)
      ├── 1. CommitIngestionService (Paginated commit history)
      ├── 2. CommitClassificationPipelineService (Deterministic classification)
      ├── 3. FileChangeIngestionService (Per-commit file patch changes)
      ├── 4. ContributorAggregationService (Author email attribution)
      ├── 5. RepositoryFileAggregationService (File revisions & code churn)
      ├── 6. RepositoryContributorFileAggregationService (File-contributor matrix)
      └── 7. RepositoryFileRiskMaterializationService (Multi-dimensional risk scoring)
      │
      ▼
PostgreSQL 16 (Source of Truth)
      │
      ├── (Best-effort caching) ──► Redis 7 (Evolution Analytics Cache)
      │
      ▼
Spring Boot REST API Layer
      │
      ▼
React 18 + Vite Analytics Dashboard (6 Interactive Views)
```

---

## Analytical Features & Modules

### 1. Repository & Analysis Jobs
- Repository registration by `owner` and `name`.
- GitHub metadata synchronization (stars, forks, primary language, open issues, visibility).
- Asynchronous analysis job submission (`PENDING` $\to$ `RUNNING` $\to$ `COMPLETED` / `FAILED`).
- Job stage tracking, execution duration measurement, and failure diagnosis.

### 2. Commit Intelligence
- Ingests repository commit histories with RFC 5988 `Link` header pagination.
- Deduplication against `(repository_id, github_commit_sha)` unique constraint.
- **Deterministic Classification Engine**: Categorizes commits into standard engineering types (`FEATURE`, `BUG_FIX`, `REFACTOR`, `DOCUMENTATION`, `TEST`, `BUILD`, `CONFIGURATION`, `DEPENDENCY`, `OTHER`) based on Conventional Commit prefixes, regex patterns, and keyword heuristics.
- Filtering and sorting by author email, classification category, and ISO-8601 date ranges.

### 3. File Intelligence & Hotspots
- Tracks individual file additions, deletions, total modifications, and file lifecycle state (`ADDED`, `MODIFIED`, `REMOVED`, `RENAMED`).
- Materializes aggregated file-level metrics: `totalRevisions`, `totalAdditions`, `totalDeletions`, `totalChurn`.
- Identifies hotspots via revision frequency and code churn ranking.
- File drilldown with directory breakdown, file extension distribution, and primary contributor identification.

### 4. Contributor Intelligence & File Ownership
- Deterministic attribution using normalized author email (`LOWER(TRIM(author_email))`) as the primary attribution key.
- Materializes contributor aggregate commits, additions, deletions, and churn.
- Computes contributor-to-file relationships and file **ownership concentration** ($\text{topContributorRevisions} / \text{totalRevisionsAcrossContributors}$).
- *Note*: Author email is used as a deterministic attribution key and does not imply verified human identity.

### 5. Risk & Stability Modeling
- **Deterministic Analytical Model**: Evaluates multi-dimensional historical change signals against repository normalization maxima:
  - **Revision Frequency ($S_{\text{rev}}$)**: Log-normalized commit frequency against repository maximum ($\text{weight} = 0.30$).
  - **Code Churn ($S_{\text{churn}}$)**: Log-normalized lines added/deleted against repository maximum ($\text{weight} = 0.30$).
  - **Recency ($S_{\text{rec}}$)**: True 90-day half-life exponential decay ($\text{weight} = 0.20$):
    $$S_{\text{rec}} = \exp\left(-\frac{\ln(2) \cdot \text{ageDays}}{90}\right)$$
  - **Ownership Concentration ($S_{\text{own}}$)**: Top contributor revision share clamped to $[0.0, 1.0]$ ($\text{weight} = 0.20$).
- **Baseline Score**: $S_{\text{baseline}} = S_{\text{rev}}$ (revision frequency only).
- **Composite Score**: Weighted multi-dimensional hotspot score:
  $$S_{\text{composite}} = 0.30 S_{\text{rev}} + 0.30 S_{\text{churn}} + 0.20 S_{\text{rec}} + 0.20 S_{\text{own}}$$
- *Important*: This is a descriptive analytical metric of historical engineering churn and concentration, **not** a machine learning model, and does **not** claim to detect security vulnerabilities or code bugs.

### 6. Repository Evolution Analytics
- Computes monthly activity buckets with zero-filled historical continuity.
- Historical period comparison (e.g., Last 30 Days vs Prior 30 Days).
- Engineering composition trends and classification breakdowns over time.

### 7. Caching Layer
- Redis caching for expensive evolution analytics endpoints.
- Versioned, composite cache keys with a 5-minute configurable TTL (`EVOLUTION_CACHE_TTL`).
- PostgreSQL remains the strict source of truth; cache failures or missing Redis instances degrade gracefully with automatic database fallback.

### 8. Production Observability & Telemetry
- Unique Correlation ID propagation (`X-Correlation-ID`) across HTTP requests, thread contexts, and log statements.
- Structured HTTP request/response logging with duration tracking.
- Analysis pipeline stage timing (`gitpulse.analysis.stage.duration`).
- Custom Micrometer application metrics (`gitpulse.analysis.jobs`, `gitpulse.commits.ingested`, `gitpulse.filechanges.ingested`, `gitpulse.cache.hits`, `gitpulse.cache.misses`, `gitpulse.cache.errors`, `gitpulse.kafka.publish.retries`).
- Spring Boot Actuator health, liveness, and readiness probes (`/actuator/health`, `/actuator/metrics`, `/actuator/info`).

---

## Frontend Analytics Dashboard

The React 18 + Vite + TypeScript frontend provides 6 cohesive analytical dashboard views:

1. **Overview Dashboard**: Repository status, GitHub metadata, sync controls, and analysis job execution history.
2. **Evolution Dashboard**: Interactive timeline charts of monthly commit activity, churn intensity, and engineering composition trends.
3. **File Intelligence & Hotspots**: Paginated file tables, code churn distributions, deleted file tracking, and file extension filters.
4. **Contributors & Ownership**: Contributor activity rankings, churn attributions, and file-level ownership share distributions.
5. **Commit Intelligence**: Engineering commit log, classification breakdowns, author filters, date-range filtering, and per-commit file diff inspections.
6. **Risk & Stability Dashboard**: Side-by-side comparison of baseline revision-frequency scores vs composite multi-dimensional hotspot scores with factor decomposition.

---

## REST API Specification

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/repositories` | Register a new GitHub repository |
| `POST` | `/api/v1/repositories/{id}/sync` | Synchronize repository metadata from GitHub |
| `GET` | `/api/v1/repositories/{id}` | Get repository details |
| `GET` | `/api/v1/repositories` | List all registered repositories |
| `POST` | `/api/v1/repositories/{id}/analysis-jobs` | Trigger an asynchronous repository analysis job |
| `GET` | `/api/v1/analysis-jobs/{jobId}` | Get status and stage details of an analysis job |
| `GET` | `/api/v1/repositories/{id}/analysis-jobs` | Get analysis job history for a repository |
| `GET` | `/api/v1/repositories/{id}/commits` | List commits (paginated, sorted, filtered by classification/author/date) |
| `GET` | `/api/v1/repositories/{id}/commits/{commitId}` | Get commit details and associated file changes |
| `GET` | `/api/v1/repositories/{id}/files` | List repository files (paginated, sorted by churn/revisions) |
| `GET` | `/api/v1/repositories/{id}/files/hotspots` | List top repository hotspots |
| `GET` | `/api/v1/repositories/{id}/files/{*filePath}` | Get file intelligence details and primary contributor |
| `GET` | `/api/v1/repositories/{id}/contributors` | List contributors (paginated, sorted by commits/churn) |
| `GET` | `/api/v1/repositories/{id}/file-ownership` | List file ownership shares across contributors |
| `GET` | `/api/v1/repositories/{id}/evolution` | Get monthly repository activity evolution |
| `GET` | `/api/v1/repositories/{id}/evolution/compare` | Compare repository activity between two time periods |
| `GET` | `/api/v1/repositories/{id}/evolution/composition` | Get commit classification composition over time |

---

## Database Migrations (Flyway V1–V9)

The schema is managed with Flyway migrations (`classpath:db/migration`):
- `V1`: `repositories` and `analysis_jobs` tables.
- `V2`: GitHub metadata columns on `repositories`.
- `V3`: `commits` table with composite indexing on `(repository_id, committed_at)`.
- `V4`: `file_changes` table with indexing on `commit_id` and `file_path`.
- `V5`: `contributors` and `repository_contributors` attribution tables.
- `V6`: `repository_files` table with aggregated churn metrics.
- `V7`: `classification` column on `commits` table.
- `V8`: `repository_contributor_files` table for file-contributor relationships.
- `V9`: Multi-dimensional risk and stability score columns on `repository_files`.

---

## Research Benchmark Harness (Hypothesis Evaluation)

### 1. Research Question & Hypothesis
> **Question**: Does a multi-dimensional file hotspot score provide superior predictive signal for future repository activity compared to revision frequency alone?

$$\text{Score}_{\text{Composite}} = 0.30 \cdot S_{\text{revisions}} + 0.30 \cdot S_{\text{churn}} + 0.20 \cdot S_{\text{recency}} + 0.20 \cdot S_{\text{ownership}}$$
$$\text{Score}_{\text{Baseline}} = S_{\text{revisions}}$$

### 2. Temporal Evaluation Methodology
- **Historical Window $(-\infty, T_{\text{cutoff}}]$**: Reconstructs historical metrics strictly using commits committed on or before $T_{\text{cutoff}}$.
- **Future Evaluation Window $(T_{\text{cutoff}}, T_{\text{cutoff}} + \text{horizon}]$**: Observes actual future activity (e.g., 90-day horizon) strictly after $T_{\text{cutoff}}$.
- **Anti-Leakage Guarantees**: Benchmark queries aggregate from immutable source logs (`commits`, `file_changes`) up to $T_{\text{cutoff}}$ and never read present-day materialized read models.

### 3. Evaluated Signals & Ablation Set
1. `baseline` ($S_{\text{revisions}}$)
2. `composite` ($0.30 S_{\text{revisions}} + 0.30 S_{\text{churn}} + 0.20 S_{\text{recency}} + 0.20 S_{\text{ownership}}$)
3. `revisionFrequency`
4. `churn`
5. `recency`
6. `ownershipConcentration`

### 4. Statistical Ranking & Evaluation Metrics
- **Headline Primary Metric**: **Precision@10 for `futureChanged`** (fraction of top 10 predicted hotspot files modified in the future window).
- **Supporting Metrics**: Precision@5, Precision@20, Recall@5, Recall@10, Recall@20, HitRate@5, HitRate@10, HitRate@20.
- **Continuous Metrics**: Spearman rank correlation ($\rho$) vs future revisions / code churn, ROC-AUC via Mann-Whitney U statistic.
- **Bootstrap Analysis**: 10,000 resamples with deterministic seed (`20260921L`) computing 95% percentile confidence intervals for paired differences ($\Delta \text{Precision@10}$).

---

## Benchmark Study Configuration & Current Status

> **Benchmark Execution Status**: **NOT YET EXECUTED** (0 real runs executed).

### Configured Study Parameters
- **Target Repositories**: 15 public repositories across diverse sizes and language ecosystems.
- **Cutoffs Per Repository**: 3 deterministic, evenly-spaced cutoffs.
- **Observation Horizon**: 90 days.
- **Total Planned Runs**: $15 \times 3 = 45$ independent repository/cutoff evaluation runs.

### Configured Evaluation Targets
1. `octocat/Hello-World` (Small — Educational / Baseline)
2. `pallets/flask` (Medium — Python WSGI Web Framework)
3. `psf/requests` (Medium — Python HTTP Client)
4. `expressjs/express` (Medium — Node.js Framework)
5. `facebook/react` (Large — Frontend UI Library)
6. `spring-projects/spring-boot` (Large — Java Enterprise Framework)
7. `vuejs/core` (Large — TypeScript / Vue Runtime)
8. `gin-gonic/gin` (Medium — Go HTTP Web Framework)
9. `rust-lang/rust-clippy` (Large — Rust Static Analysis)
10. `axios/axios` (Medium — JavaScript HTTP Client)
11. `torvalds/linux` (Large — C Systems Kernel)
12. `curl/curl` (Large — C Networking Utility)
13. `tiangolo/fastapi` (Medium — Python Asynchronous Web Framework)
14. `chalk/chalk` (Small — Node.js Terminal Utility)
15. `google/guava` (Large — Java Core Utilities)

*Note*: These 15 repositories represent the pre-configured benchmark dataset specification. Real ingestion and live execution of these 45 runs will occur upon local infrastructure and data bootstrap.

---

## Local Development & Setup

### Prerequisites
- **Java**: 21+
- **Node.js**: 18+ (with npm)
- **Docker & Docker Compose**: for PostgreSQL 16, Kafka KRaft, and Redis 7
- **Maven**: 3.9+ (or use `./mvnw` / `.\mvnw.cmd`)
- **GitHub Token**: Optional for development; recommended for large repository ingestion (set `GITHUB_TOKEN` in `.env`).

### 1. Start Infrastructure (PostgreSQL, Kafka KRaft & Redis)
```bash
# Start containers in background
docker compose up -d

# Verify container health
docker compose ps
```

### 2. Run Automated Test Suite
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
The frontend dashboard starts at `http://localhost:3000` and proxies API requests to `http://localhost:8080`.

### 5. Build Frontend for Production
```bash
cd frontend
npm run build
```

### 6. Stop Infrastructure
```bash
docker compose down
```

---

## Verification & Test Status

- **Backend Automated Tests**: **533 tests passing** (0 failures, 0 errors, 0 skipped).
- **Frontend Production Build**: Clean build with zero TypeScript or bundle compilation errors (`tsc && vite build`).
- **Working Tree**: Clean on branch `main`.

*Note*: The automated test suite verifies all backend services, asynchronous Kafka consumers, Redis caching mechanisms, Flyway migrations, API contracts, and mathematical benchmark algorithms using in-memory H2 and Embedded Kafka. It does not represent live 15-repository benchmark study results.

---

## Project Roadmap

### Completed Milestones
- [x] Spring Boot 3 + Java 21 backend architecture & domain modeling
- [x] GitHub REST API integration with rate-limit and pagination handling
- [x] Apache Kafka KRaft event-driven asynchronous analysis pipeline
- [x] Commit ingestion, deduplication, and deterministic classification
- [x] File change tracking, churn calculation, and hotspot identification
- [x] Contributor attribution and file ownership concentration modeling
- [x] Deterministic multi-dimensional risk and stability scoring engine
- [x] Monthly repository evolution analytics and period comparisons
- [x] Redis caching layer with graceful PostgreSQL database fallback
- [x] Observability, correlation IDs, Micrometer metrics, and Actuator health probes
- [x] Dependency health resilience and error-handling audit
- [x] API validation and security hardening
- [x] 6-view interactive React + TypeScript + Vite analytics dashboard
- [x] Temporal benchmark methodology and statistical calculation engine
- [x] 15-repository benchmark dataset configuration and deterministic cutoff selector

### In Progress / Upcoming Milestones
- [ ] Local benchmark infrastructure bootstrap (Docker / PostgreSQL / Kafka)
- [ ] Real repository history ingestion for the 15 benchmark targets
- [ ] Execution of 45 planned benchmark runs ($15 \text{ repos} \times 3 \text{ cutoffs}$)
- [ ] Export of real benchmark research CSVs and distribution summaries
- [ ] Generation of research visualizations from measured benchmark data
- [ ] Neutral empirical evaluation and research findings report

---

## Known Limitations

- **GitHub API Rate Limits**: Unauthenticated GitHub API calls are limited by GitHub to 60 requests/hour per IP. For ingesting large repositories, set `GITHUB_TOKEN` in `.env` (5,000 requests/hour).
- **Per-Commit File Limit**: The GitHub REST API commit-detail endpoint returns a maximum of 300 changed files per commit. Bulk commits exceeding 300 files capture the first 300 files.
- **Docker Requirement for Local Daemons**: Running full asynchronous analysis locally requires Docker / Compose for Kafka, PostgreSQL, and Redis. The automated test suite (`./mvnw test`) runs fully offline in-memory using Embedded Kafka and H2.
- **Authentication**: GitPulse currently operates in single-tenant local/internal mode without user authentication or RBAC.