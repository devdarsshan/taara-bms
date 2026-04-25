# Docker Setup

This repository can be started locally as a single application container that serves both the Angular frontend and the Spring Boot backend on `http://localhost:8080`.

Related guides:

- [Docker Hub Publish Guide](F:/Gen_Projects/taara-bms/DOCKER_HUB_PUBLISH.md)
- [Docker Desktop Client Guide](F:/Gen_Projects/taara-bms/DOCKER_HUB_CLIENT_GUIDE.md)

## What This Includes

- One Docker image built from the repo root
- Angular frontend baked into the Spring Boot jar and served on the same origin as `/api`
- Runtime-generated `/app-config.js` from container environment variables
- Existing hosted Supabase auth and hosted Postgres kept as the system of record

## First-Time Setup

1. Copy `.env.docker.example` to `.env.docker`
2. Fill in the database and Supabase values
3. Keep the bootstrap admin values if you want an admin user created automatically on first start

## Start With Docker Compose

```bash
docker compose up --build
```

Open `http://localhost:8080`.

## Start With Docker Run

```bash
docker build -t taara-bms .
docker run --env-file .env.docker -p 8080:8080 taara-bms
```

## Runtime Variables

Required:

- `TAARA_DB_URL`
- `TAARA_DB_USERNAME`
- `TAARA_DB_PASSWORD`
- `TAARA_SUPABASE_URL`
- `TAARA_SUPABASE_ANON_KEY`
- `TAARA_SUPABASE_SERVICE_ROLE_KEY`
- `TAARA_SUPABASE_JWT_ISSUER`
- `TAARA_SUPABASE_JWT_JWKS_URL`

Recommended for first login:

- `TAARA_BOOTSTRAP_ADMIN_EMAIL`
- `TAARA_BOOTSTRAP_ADMIN_PASSWORD`

Optional:

- `TAARA_CORS_ALLOWED_ORIGINS`
- `TAARA_API_BASE_URL`
- `PORT`

## Notes

- The container expects internet access so it can reach hosted Supabase and the hosted database.
- Frontend deep links such as `/overview` are served by Spring Boot and fall back to the Angular `index.html`.
- The checked-in `bms-FE/public/app-config.js` remains useful for local frontend-only development; the containerized app uses the backend-generated `/app-config.js` instead.
