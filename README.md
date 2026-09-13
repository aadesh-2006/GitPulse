# GitPulse

GitPulse is a GitHub Repository Activity Intelligence platform that analyzes repository history to understand code evolution, hotspots, contributor activity, and engineering-risk indicators.

> **Note**: GitPulse is currently being built incrementally. This repository currently contains the backend foundation (Step 1).

---

## Current Technology Stack

- **Runtime & Language**: Java 21
- **Framework**: Spring Boot 3.3.x (Spring Web, Spring Data JPA, Spring Validation, Spring Boot Actuator)
- **Database**: PostgreSQL 16 (configured via Spring Data JPA / HikariCP)
- **Build Tool**: Maven (with Maven Wrapper)
- **Infrastructure**: Docker & Docker Compose (local PostgreSQL container)
- **Testing**: Spring Boot Test, JUnit 5, H2 (test scope)

---

## Current Development Status

- **Step 1 — Backend Foundation**: Initialized core Spring Boot application, domain package layout, configuration management, centralized exception handling, Docker Compose database setup, and test harness.
- **Future Steps**: Ingestion pipelines, GitHub API & Webhook integration, Kafka event streaming, Redis caching, analytical hotspot computation, and React frontend will be introduced in subsequent milestones.

---

## Project Structure

```text
gitpulse/
├── .env.example
├── .gitignore
├── docker-compose.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
├── src/
│   ├── main/
│   │   ├── java/com/gitpulse/
│   │   │   ├── GitPulseApplication.java
│   │   │   ├── common/
│   │   │   │   └── exception/
│   │   │   │       ├── AppException.java
│   │   │   │       ├── ResourceNotFoundException.java
│   │   │   │       ├── ErrorResponse.java
│   │   │   │       └── GlobalExceptionHandler.java
│   │   │   └── domain/
│   │   │       ├── analysis/
│   │   │       ├── analytics/
│   │   │       ├── commit/
│   │   │       ├── contributor/
│   │   │       └── repository/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test/
│       ├── java/com/gitpulse/
│       │   └── GitPulseApplicationTests.java
│       └── resources/
│           └── application-test.yml
```

---

## Local Setup Prerequisites

- **Java Development Kit (JDK)**: Java 21 or later
- **Docker & Docker Compose**: For running the local PostgreSQL container
- **Maven**: Version 3.9+ (or use the included `./mvnw` / `mvnw.cmd` wrapper)

---

## Getting Started

### 1. Configure Environment

Copy `.env.example` to create your local environment file if needed:

```bash
cp .env.example .env
```

Environment variables supported:

| Variable | Description | Default |
|---|---|---|
| `SERVER_PORT` | Port for the Spring Boot server | `8080` |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | PostgreSQL database name | `gitpulse` |
| `DB_USERNAME` | PostgreSQL username | `gitpulse` |
| `DB_PASSWORD` | PostgreSQL password | `gitpulse` |

### 2. Start PostgreSQL via Docker Compose

Start the PostgreSQL service:

```bash
docker compose up -d
```

To stop PostgreSQL:

```bash
docker compose down
```

### 3. Build the Project

Run tests and compile:

```bash
./mvnw clean test
```

Package the application:

```bash
./mvnw clean package
```

### 4. Run the Spring Boot Application

Using Maven:

```bash
./mvnw spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar target/gitpulse-0.0.1-SNAPSHOT.jar
```

### 5. Health Check

Once the application is running, verify its health endpoint:

```bash
curl http://localhost:8080/actuator/health
```