🚀 Spring Boot Kotlin Initial Template

A production-grade multi-module Spring Boot 3.x + Kotlin starter template.
This project includes the essential building blocks for real-world backend services such as:

* API Response standardization
* Global exception handling
* JWT authentication
* Module separation (api / application / domain / batch)
* Logging with MDC
* JPA configuration
* Swagger UI
* Environment-specific profiles

You can run this project immediately, then customize the package name and DB settings to fit your service.


🔧 Required Customization Before Use

This template is prepared for public sharing.
If you plan to use it for your own service, you must update the following items.


1️⃣ Change Base Package (com.tribe → your domain)
All modules (api / application / domain / batch) use package com.tribe.
Rename it to your organization or project domain.

  IntelliJ shortcut:
Right-click package → Refactor → Rename (Shift + F6)
Imports will update automatically.

2️⃣ Configure Your Own Database
The template uses H2 for easy execution.
Replace it with your actual DB (MySQL/PostgreSQL/etc)

src/main/resources/application-local.yml:
spring:
  datasource:
    url: jdbc:h2:mem:tribe-local;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver

Local profile now boots against an embedded H2 database by default.
Override `SPRING_DATASOURCE_*` only when you want to point local at PostgreSQL instead.
Local also uses `spring.jpa.hibernate.ddl-auto=create` by default, so the schema is recreated from entities on every startup.
For shared dev/prod environments with `ddl-auto: none`, apply schema changes manually before rollout.
Example: `backend/api/src/main/resources/db/manual/20260414_add_trip_region_code.sql`

3️⃣ Replace JWT Secret Key
A sample secret is included.
Generate a new one:

openssl rand -hex 32

Set it inside your application-local.yml / application-dev.yml.

4️⃣ Update Swagger Info
Modify SwaggerConfig.kt to match your project branding:
.info(
  Info()
    .title("Your API")
    .description("Your project description")
)

5️⃣ Redis (Optional)

Redis dependency is included.
If your service doesn’t use Redis, simply remove:

implementation("org.springframework.boot:spring-boot-starter-data-redis")


▶ How to Run
⭐ Recommended: IntelliJ Run Button
Runs with the application-local profile by default.

⭐ Official Method: Gradle bootRun
./gradlew :api:bootRun

## Local AI generation test

The AI frontend worktree uses port `8082`; this local API uses `8083` to avoid an IntelliJ port conflict.
Requires Java 21, Redis, and Ollama. The local script uses `gemma3:4b` by default; override with `OLLAMA_MODEL` for another installed model.

Start Ollama and Redis in separate terminals:

```bash
OLLAMA_CONTEXT_LENGTH=8192 OLLAMA_NUM_PARALLEL=1 ollama serve
redis-server --bind 127.0.0.1 --port 6381 --save '' --appendonly no
```

Download the model once, then start the API from `backend/`:

```bash
ollama pull gemma3:4b
bash scripts/run-local-ai.sh
```

Set these values in `frontend/.env.development.local`, then run `npm run dev -- --host 127.0.0.1 --port 8082 --strictPort` from `frontend/`:

```dotenv
VITE_API_BASE_URL=http://localhost:8083/api/v1
VITE_BACKEND_ORIGIN=http://localhost:8083
```

Open `http://localhost:8082` and log in with `seed.owner@tribe.local` / `password`.
The script always uses an isolated in-memory H2 database and enables the existing seed accounts; restarting the API resets local trips and proposals.
Choose **AI 여행 만들기**, enter a region, 1–5 days, companion type, and 1–3 styles, then generate and apply the proposal.
If `GOOGLE_KEY` is absent, Google Places lookup is disabled for this local run. Leave **지도에서 찾지 못한 장소도 텍스트 일정으로 저장** checked to test actual AI generation and itinerary persistence without map matching.
Ollama model setup follows the [official CLI documentation](https://docs.ollama.com/cli).
