# MyLedger

A lightweight, personal web budget and spending analytics application tailored to individual spending habits (solo vs. social dining breakdown, fixed vs. variable expense tracking, and multi-period cash flow analytics). All UI text, notifications, and stored category names are implemented in English.

## Tech Stack
- **Backend:** Java 21 (bytecode target; builds on JDK 21/24), Spring Boot 3.5, Spring Web, Spring Data JPA, Spring Validation, Spring Security
- **Frontend:** Thymeleaf, Tailwind CSS, Chart.js (responsive for macOS desktop & mobile)
- **Database:** PostgreSQL (Neon, Supabase, or local Docker PostgreSQL)
- **Build & Deploy:** Gradle Wrapper, Docker & Docker Compose

## Engineering Highlights
- **Exact Monetary Precision:** `BigDecimal` in Java and `DECIMAL(12, 0)` (KRW) in PostgreSQL — zero `float` or `double` arithmetic.
- **Database-Level Aggregation:** All dashboard KPIs, category breakdowns, companion comparisons, fixed/variable ratios, and 12-month trends are aggregated in SQL/JPQL via `GROUP BY`.
- **Composite Indexing:** `idx_transactions_date_type` on `transactions(transaction_date, type)`.
- **Single-User Security:** Spring Security form login backed by BCrypt and `.env`-configured credentials.
- **Automatic Category Seeding:** Inserts the 5 English `INCOME` and 10 English `EXPENSE` categories on first startup if missing.

## Quick Start

### 1. Configure Environment Variables
```bash
cp .env.example .env
```
Edit `.env` with your PostgreSQL connection (Neon / Supabase / local Docker) and login credentials (`APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD`).

### 2. Run Locally with Docker Compose (App + PostgreSQL)
```bash
docker compose up --build
```
Open `http://localhost:8080` and sign in with your configured credentials.

### 3. Run Locally with Gradle (External Neon/Supabase PostgreSQL)
Export variables from `.env` and start Spring Boot:
```bash
set -a && source .env && set +a
./gradlew bootRun
```

### 4. Instant Local Preview (Embedded H2 in PostgreSQL Mode)
For quick local testing without starting PostgreSQL:
```bash
./gradlew bootRun --args='--spring.profiles.active=local-h2'
```
Default local credentials (when `.env` is not set): `admin` / `myledger1234`.

## Running Tests
```bash
./gradlew test
```

## Roadmap
- **Phase 1 (Implemented):** Single-user authentication, quick transaction entry with dynamic English category filtering, monthly transaction management (filter/edit/delete), and Daily/Weekly/Monthly/Yearly analytics dashboard with Chart.js.
- **Phase 2 (Planned):** Recurring transactions (`recurring_transactions` + `@Scheduled` 00:05 batch job), CSV export, and extended edge-case unit test coverage.
