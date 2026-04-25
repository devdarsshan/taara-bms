# Docker Hub Publish Guide

This guide is for the maintainer who builds the Taara BMS image and publishes it to Docker Hub for other systems to use.

It assumes:

- the application already works locally in Docker
- you have a Docker Hub account
- Docker Desktop is installed and signed in

## Goal

Publish one image to Docker Hub so a client can pull it from Docker Desktop and run it with their own private environment values.

## Important Rules

- Never bake database or Supabase secrets into the image.
- Never commit `.env.docker` to Git.
- Never put secrets into the Docker Hub repository description.
- Treat the Docker image as public code plus runtime behavior, not as a secret store.
- Share secrets with the client separately through a secure channel such as a password manager, encrypted note, or other approved private method.

## Files You Already Have

- [Dockerfile](F:/Gen_Projects/taara-bms/Dockerfile)
- [docker-compose.yml](F:/Gen_Projects/taara-bms/docker-compose.yml)
- [.env.docker.example](F:/Gen_Projects/taara-bms/.env.docker.example)
- [DOCKER.md](F:/Gen_Projects/taara-bms/DOCKER.md)

## Decide Your Image Name

Use one permanent image name, for example:

`yourdockerhubusername/taara-bms`

Use two kinds of tags:

- a fixed version tag such as `1.0.0` or `2026.04.03`
- a moving tag called `latest`

Recommended pattern:

- `yourdockerhubusername/taara-bms:1.0.0`
- `yourdockerhubusername/taara-bms:latest`

## One-Time Docker Hub Setup

1. Sign in to [Docker Hub](https://hub.docker.com/).
2. Create a repository named `taara-bms`.
3. Choose repository visibility:
   - `Public` if your client should be able to pull without being invited
   - `Private` if only approved Docker Hub users should access it
4. Sign in to Docker Desktop with the same Docker Hub account.

## Secrets You Must Keep Outside The Image

These values belong on the machine that runs the container, not in Docker Hub:

- `TAARA_DB_URL`
- `TAARA_DB_USERNAME`
- `TAARA_DB_PASSWORD`
- `TAARA_SUPABASE_URL`
- `TAARA_SUPABASE_ANON_KEY`
- `TAARA_SUPABASE_SERVICE_ROLE_KEY`
- `TAARA_SUPABASE_JWT_ISSUER`
- `TAARA_SUPABASE_JWT_JWKS_URL`
- `TAARA_BOOTSTRAP_ADMIN_EMAIL`
- `TAARA_BOOTSTRAP_ADMIN_PASSWORD`

## How To Prepare The Client Secret Pack

Create a private document for the client that contains:

- the Docker Hub image name
- the tag they should use, usually `latest`
- the full environment variable list
- the initial admin email and password if you are bootstrapping an admin
- the expected application URL, usually `http://localhost:8080`

Do not upload this secret pack to Git, Docker Hub, or a shared public drive.

## Publish Workflow

This is the most reliable publish path.

1. Open Docker Desktop and confirm the local image works.
2. Open a terminal in the repository root.
3. Build the image with a version tag.
4. Tag the same image as `latest`.
5. Push both tags to Docker Hub.

Example commands:

```bash
docker build -t yourdockerhubusername/taara-bms:1.0.0 .
docker tag yourdockerhubusername/taara-bms:1.0.0 yourdockerhubusername/taara-bms:latest
docker push yourdockerhubusername/taara-bms:1.0.0
docker push yourdockerhubusername/taara-bms:latest
```

If you prefer date-based versions:

```bash
docker build -t yourdockerhubusername/taara-bms:2026.04.03 .
docker tag yourdockerhubusername/taara-bms:2026.04.03 yourdockerhubusername/taara-bms:latest
docker push yourdockerhubusername/taara-bms:2026.04.03
docker push yourdockerhubusername/taara-bms:latest
```

## Docker Desktop UI Notes

Docker Desktop is excellent for:

- signing in to Docker Hub
- viewing local images
- deleting old images
- pushing images that already have the right repository tag
- confirming whether a pushed tag exists

For repeatable publishing, the terminal is still the safest way to build and tag exactly what you want.

## Verify The Published Image

After pushing:

1. Open Docker Hub in the browser.
2. Open the `taara-bms` repository.
3. Confirm the new tag appears.
4. Optionally pull that exact tag on another machine before sharing it with the client.

## Recommended Release Routine

For each new release:

1. Test locally with your real `.env.docker`.
2. Publish a new fixed version tag.
3. Move `latest` to the same image.
4. Tell the client either:
   - pull `latest` if they always want the newest build
   - pull a fixed version tag if you want a safer controlled rollout

## Rollback Plan

If `latest` has a problem:

1. Tell the client to stop using `latest`.
2. Give them the last known good fixed version tag.
3. They can pull and run that older tag from Docker Desktop.

Example:

```bash
docker pull yourdockerhubusername/taara-bms:1.0.0
```

## What Not To Do

- Do not put `.env.docker` inside the image.
- Do not hardcode secrets into the Dockerfile.
- Do not rely on `latest` as your only tag.
- Do not delete old working tags immediately after a release.

## Maintainer Checklist

- Docker Hub repository exists
- Image builds locally
- Image runs locally
- Fixed version tag pushed
- `latest` tag pushed
- Client secret pack prepared
- Client guide shared
