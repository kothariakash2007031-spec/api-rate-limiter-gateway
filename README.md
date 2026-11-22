# API Rate Limiter & Usage Analytics Gateway

A small API gateway built with **Spring Boot**, **Spring Cloud Gateway**, **Redis** and **PostgreSQL**.
Clients call the gateway with an API key. The gateway checks the key, applies the client's plan limit
using a Redis counter, and only then forwards the request to the protected backend API.

## What it does

- Registers **clients** and **plans** (for example Free = 10 requests per minute).
- Generates **API keys**, stores only a **SHA-256 hash**, and supports **rotate** and **revoke**.
- A **gateway** (port 9090) rejects requests with a missing, invalid, expired or revoked key (**401**).
- A **fixed-window rate limiter** backed by Redis returns **429 Too Many Requests** when a client goes over its plan limit.
- Responses include `X-RateLimit-Limit`, `X-RateLimit-Remaining` and `Retry-After` headers.

## Architecture

```
Client (X-API-Key header)
        |
        v
Gateway  :9090  (Spring Cloud Gateway)
   1. ApiKeyAuthFilter  -> asks backend "is this key valid, which plan?"   -> 401 if not
   2. RateLimitFilter   -> INCR counter in Redis for this client/minute     -> 429 if over limit
   3. Route /api/**     -> forwards to the protected backend
        |
        v
Backend  :8080  (Spring Boot)  ->  PostgreSQL
   - /admin/**       management APIs (clients, plans, keys)
   - /internal/**    key validation used by the gateway
   - /api/**         sample protected API (users, products)

Redis :6379  -> short-lived rate-limit counters (auto-expire after 60 seconds)
```

## Tech stack

| Layer | Technology |
| --- | --- |
| Language | Java 25 |
| Backend | Spring Boot 4.1.1, Spring Web, Spring Data JPA, Validation |
| Gateway | Spring Cloud Gateway (WebFlux), Spring Data Reactive Redis |
| Database | PostgreSQL 17 |
| Rate-limit state | Redis 7 (run with Docker) |
| Testing | Postman |

## Project structure

```
ratelimiter-backend/      Spring Boot app, port 8080
  controller/             REST endpoints (Health, SampleApi, AdminClient, AdminPlan, AdminApiKey, InternalKey)
  service/                Business rules (ClientService, PlanService, ApiKeyService)
  repository/             Spring Data JPA interfaces
  entity/                 Client, Plan, ApiKey (database tables)
  dto/                    Request / response objects
  security/               ApiKeyGenerator (random key + SHA-256 hash)

gateway-service/          Spring Cloud Gateway app, port 9090
  filter/                 ApiKeyAuthFilter, RateLimitFilter
  model/                  KeyInfo (what the backend returns for a valid key)
```

## Database design

| Table | Columns |
| --- | --- |
| `clients` | id, name, email (unique), status, created_at, plan_id |
| `plans` | id, name (unique), requests_per_minute, daily_quota |
| `api_keys` | id, client_id, key_hash (unique), key_prefix, status, created_at, expires_at |

Tables are created automatically by Hibernate (`spring.jpa.hibernate.ddl-auto=update`, suitable for development only).

## Running it locally

**Prerequisites:** Java 17+ (tested on 25), PostgreSQL, Docker Desktop, Postman, Eclipse (or any IDE).

1. **Create the database** in pgAdmin or psql:
   ```sql
   CREATE DATABASE ratelimiter_db;
   ```
2. **Start Redis:**
   ```
   docker run -d --name redis -p 6379:6379 redis:7
   docker exec -it redis redis-cli ping      # expect PONG
   ```
   After a reboot, start it again with `docker start redis`.
3. **Configure the backend** (`ratelimiter-backend/src/main/resources/application.properties`):
   ```properties
   server.port=8080
   spring.datasource.url=jdbc:postgresql://localhost:5432/ratelimiter_db
   spring.datasource.username=postgres
   spring.datasource.password=YOUR_PASSWORD
   spring.jpa.hibernate.ddl-auto=update
   spring.data.redis.host=localhost
   spring.data.redis.port=6379
   ```
4. **Configure the gateway** (`gateway-service/src/main/resources/application.properties`):
   ```properties
   server.port=9090
   spring.cloud.gateway.server.webflux.routes[0].id=sample-backend
   spring.cloud.gateway.server.webflux.routes[0].uri=http://localhost:8080
   spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/**
   backend.base-url=http://localhost:8080
   spring.data.redis.host=localhost
   spring.data.redis.port=6379
   ```
5. **Run the backend first**, then the gateway (main class of each project).

## API endpoints

### Backend (port 8080)

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/health` | Health check |
| POST | `/admin/clients` | Register client |
| GET | `/admin/clients`, `/admin/clients/{id}` | List / inspect clients |
| PUT | `/admin/clients/{id}/plan/{planId}` | Assign a plan to a client |
| POST | `/admin/plans` | Create plan |
| GET | `/admin/plans` | List plans |
| POST | `/admin/clients/{id}/keys` | Generate API key (shown once) |
| GET | `/admin/clients/{id}/keys` | List keys (prefix and status only) |
| POST | `/admin/keys/{id}/rotate` | Rotate key (old key revoked) |
| DELETE | `/admin/keys/{id}` | Revoke key |
| POST | `/internal/keys/validate` | Used by the gateway to validate a key |
| GET | `/api/users`, `/api/products` | Sample protected API |

### Gateway (port 9090)

| Method | Endpoint | Result |
| --- | --- | --- |
| GET | `/api/**` without `X-API-Key` | 401 `Missing API key` |
| GET | `/api/**` with a bad or revoked key | 401 `Invalid or revoked API key` |
| GET | `/api/**` with a valid key, under the limit | 200 plus `X-RateLimit-*` headers |
| GET | `/api/**` with a valid key, over the limit | 429 plus `Retry-After` |

## How it works

### API key security
- A key looks like `rl_` followed by 32 random bytes (from `SecureRandom`, URL-safe Base64).
- Only the **SHA-256 hash** is stored in PostgreSQL. The full key is returned once, at creation or rotation.
- A short `key_prefix` is kept so keys can be recognised in a list without exposing them.
- Keys have an expiry (90 days by default) and a status: `ACTIVE` or `REVOKED`.

### Fixed-window rate limiting
- Redis key: `rl:{clientId}:{minuteNumber}`, where `minuteNumber = currentEpochSeconds / 60`.
- Each request runs `INCR` on that key, which is atomic in Redis, so simultaneous requests get different counts.
- The first request in a window sets a 60-second expiry, so old counters delete themselves.
- If the count is above the plan's `requests_per_minute`, the gateway returns 429 with `Retry-After` set to the seconds left in the window.
- The limit is **per client**, so all keys belonging to one client share one counter.

## Test results (Postman)

| Test | Result |
| --- | --- |
| `GET /health` | 200 `{"status":"UP"}` |
| Create client, duplicate email, invalid body | 201, 409, 400 |
| Create plans, duplicate plan name | 201, 409 |
| Create, list, rotate, revoke key | 201, 200, 200, 204 |
| Gateway, no key | 401 `Missing API key` |
| Gateway, fake key | 401 `Invalid or revoked API key` |
| Gateway, valid key | 200, `X-RateLimit-Limit: 10`, `X-RateLimit-Remaining` counting down |
| Gateway, 11th request in a minute | 429 `Rate limit exceeded`, `Retry-After` header |
| After the window passes | 200 again, `Remaining` back to 9 |

## Known limitations and future work

- **Admin and internal endpoints are not authenticated yet.** `/admin/**` and `/internal/**` must be protected (Spring Security or network rules) before any real deployment.
- Only the **fixed-window** algorithm is implemented. It can allow a burst of up to twice the limit around a window boundary. Sliding window and token bucket would fix this.
- `daily_quota` is stored in the plan but **not enforced yet**.
- Requests are **not recorded** for analytics, and there is no dashboard yet.
- If **Redis is down**, the gateway currently returns an error instead of deliberately allowing or blocking traffic (a fail-open vs fail-closed decision).
- `INCR` and `EXPIRE` are separate commands. A Lua script would make them one atomic step.
- The gateway calls the backend to validate a key on every request. A short-lived cache would reduce that load.
- No IP-based limiting for requests that carry no valid key.
- Validation error responses are bare (status only). Standardised error bodies are still to do.
- Docker Compose packaging, automated tests, and load testing are not done yet.
- `ddl-auto=update` is for development only.

## Security notes

- Never commit your real database password or real API keys to GitHub. Use placeholders in `application.properties` before pushing.
