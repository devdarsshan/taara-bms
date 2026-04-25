# Docker Desktop Client Guide

This guide is for a non-technical client who only needs to pull the Taara BMS image from Docker Hub and run it using Docker Desktop.

No terminal commands are required if someone has already published the image for you.

## What You Need

- Docker Desktop installed and running
- internet access
- the Docker Hub image name, for example `yourdockerhubusername/taara-bms:latest`
- a private list of environment values from the maintainer

## Very Important

- The Docker image does not contain your private database or Supabase credentials.
- You must receive those values separately from the maintainer.
- Do not send those credentials to other people.
- Do not post screenshots that show the secret values.

## The Secret Values You Will Receive

You will usually receive values like these:

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

You may also receive:

- `PORT`
- `TAARA_CORS_ALLOWED_ORIGINS`
- `TAARA_API_BASE_URL`

If you are not given optional values, leave them alone unless the maintainer tells you otherwise.

## Before You Start

1. Open Docker Desktop.
2. Wait until Docker Desktop shows that it is running.
3. If the image repository is private, sign in with the Docker Hub account that has access.

## Step 1: Pull The Image

1. In Docker Desktop, open the `Images` section.
2. Find the option to pull an image from Docker Hub.
3. Enter the image name exactly as the maintainer gave it to you.
4. Example image name:

   `yourdockerhubusername/taara-bms:latest`

5. Start the pull.
6. Wait until the image appears in your local image list.

## Step 2: Run The Image

1. In the `Images` section, find the pulled `taara-bms` image.
2. Click `Run`.
3. If Docker Desktop shows `Optional settings`, open them.
4. Set the container name to:

   `taara-bms`

5. Set the port mapping so your machine uses:

   `8080` on the host

   and

   `8080` in the container

6. Add the environment variables one by one using the secret values you received from the maintainer.

## Step 3: Add The Environment Variables

Add each environment variable carefully.

Example format:

- Name: `TAARA_DB_URL`
- Value: `jdbc:postgresql://...`

Repeat for all required values.

If the maintainer gave you a bootstrap admin email and password, add those too.

## Step 4: Start The Container

1. Click `Run`.
2. Wait for Docker Desktop to show the container in the `Containers` section.
3. Open the logs if needed.
4. When the app is ready, open:

   [http://localhost:8080](http://localhost:8080)

## First Login

Use the admin email and password provided by the maintainer.

If login fails:

- confirm every environment variable was entered exactly
- confirm Docker Desktop is running
- confirm your internet connection works
- confirm the backend can reach the hosted database and Supabase services

## How To Update To The Latest Version

When the maintainer tells you a new version is available:

1. Open Docker Desktop.
2. Go to `Containers`.
3. Stop the current `taara-bms` container.
4. Delete the old container.
5. Go to `Images`.
6. Pull the new image tag, usually `latest`.
7. Run the new image again.
8. Enter the same environment variables you used before.
9. Open [http://localhost:8080](http://localhost:8080) and confirm the app works.

## Safer Update Option

If the maintainer gives you a fixed tag such as `1.0.0`, use that exact tag instead of `latest`.

This is safer because it avoids accidentally pulling an image that changed unexpectedly.

Example:

`yourdockerhubusername/taara-bms:1.0.0`

## How To Roll Back

If the newest version has a problem:

1. Stop and delete the current container.
2. Pull the previous working tag from Docker Hub.
3. Run that older image again with the same environment values.

Ask the maintainer for the exact tag to use.

## Where To Keep The Secret Values

Best options:

- a password manager
- an encrypted note
- a secure document only approved users can access

Less safe options:

- plain text files on the desktop
- screenshots in chat apps
- email threads forwarded to many people

## What The Client Should Never Change

- Do not edit the Docker image name unless the maintainer tells you to.
- Do not edit secret values unless the maintainer gives replacements.
- Do not delete the hosted database or Supabase credentials.
- Do not share service-role keys with anyone who does not need them.

## Troubleshooting

### The site does not open on localhost:8080

- Check that the container is running.
- Check that port `8080` is mapped.
- Check the container logs in Docker Desktop.

### Login fails

- Check the Supabase-related environment values.
- Check the admin email and password.
- Check internet connectivity.

### The container starts and stops immediately

- Open logs in Docker Desktop.
- Check the database values.
- Check whether a required environment value was missed.

### The update did not change anything

- Make sure you pulled the new tag before running a new container.
- If needed, delete the old image and pull again.

## Quick Checklist

- Docker Desktop is running
- Correct image tag was pulled
- Port `8080` is mapped
- All environment variables were entered
- The container is running
- `http://localhost:8080` opens successfully
