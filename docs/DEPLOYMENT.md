# Deployment

Operational notes for getting Echel Planner in front of real users. The primary deploy target is **AWS** — ECS Express Mode (Fargate) for the backend behind an ALB, S3 + CloudFront for the frontend SPA. A self-host fallback via `compose.deploy.yml` is also documented.

The first half of this file is the **clickops runbook** for standing up the AWS environment from scratch. After Phase 7 the CI workflow at `.github/workflows/deploy.yml` takes over and every merge to `main` deploys automatically.

---

## Architecture

```
   echelplanner.com / www.echelplanner.com           api.echelplanner.com
                 │                                            │
            Route 53 alias                              Route 53 alias
                 │                                            │
         CloudFront distribution                    Application Load Balancer
                 │                                       (HTTPS, ACM cert)
      ┌──────────┴──────────┐                              │
      │   S3 (private,      │                              ▼
      │   OAC-fronted)      │                       ECS Fargate service
      │   echelplanner-     │                       (Express Mode)
      │   frontend-prod     │                       Container from ECR :latest
      └─────────────────────┘                       planner-backend
                                                              │
                                                              │ TCP 5432
                                                              ▼
                                                    RDS Postgres t4g.micro
                                                    (default VPC, private)
                                                              │
                                                              ▼
                                                    Secrets Manager
                                                    planner/db-* + planner/jwt-secret
```

Single AWS region: **us-east-1**. CloudFront requires its ACM certificate in us-east-1, which is the reason we don't split regions.

## Phase 1 — Domain (Route 53)

1. **Console → Route 53 → Registered domains → Register domain.**
2. Search `echelplanner.com`. Add to cart, `$13/yr`. Use real WHOIS contact info (Route 53 obscures it by default).
3. Submit. Registration usually completes in minutes; the hosted zone is created automatically and you can move on to other phases while it propagates.

## Phase 2 — IAM user for CI

The deploy workflow uses an IAM user with access keys. (OIDC federation is a worthwhile upgrade later — see [Follow-ups](#follow-ups) — but access keys are the shortest path to a first deploy.)

1. **Console → IAM → Users → Create user.**
   - Name: `github-actions-deploy`
   - Do **not** check "Provide user access to the AWS Management Console."
2. Attach a single inline policy (Permissions → Add permissions → Create inline policy → JSON). Paste:

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Sid": "EcrPush",
         "Effect": "Allow",
         "Action": [
           "ecr:GetAuthorizationToken",
           "ecr:BatchCheckLayerAvailability",
           "ecr:BatchGetImage",
           "ecr:GetDownloadUrlForLayer",
           "ecr:InitiateLayerUpload",
           "ecr:UploadLayerPart",
           "ecr:CompleteLayerUpload",
           "ecr:PutImage"
         ],
         "Resource": "*"
       },
       {
         "Sid": "EcsDeploy",
         "Effect": "Allow",
         "Action": [
           "ecs:UpdateService",
           "ecs:DescribeServices"
         ],
         "Resource": "*"
       },
       {
         "Sid": "S3FrontendSync",
         "Effect": "Allow",
         "Action": ["s3:ListBucket"],
         "Resource": "arn:aws:s3:::echelplanner-frontend-prod"
       },
       {
         "Sid": "S3FrontendObjects",
         "Effect": "Allow",
         "Action": ["s3:PutObject", "s3:DeleteObject", "s3:GetObject"],
         "Resource": "arn:aws:s3:::echelplanner-frontend-prod/*"
       },
       {
         "Sid": "CloudFrontInvalidate",
         "Effect": "Allow",
         "Action": ["cloudfront:CreateInvalidation"],
         "Resource": "*"
       }
     ]
   }
   ```

   Name it `planner-ci-deploy`.
3. **Create access key.** Users → `github-actions-deploy` → Security credentials → Create access key → "Application running outside AWS." Copy the **Access key ID** and **Secret access key** — the secret is shown once.
4. **GitHub → repo Settings → Secrets and variables → Actions → New repository secret.** Add two secrets:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`

## Phase 3 — JWT secret in Secrets Manager

The backend reads its JWT signing secret from Secrets Manager at ECS task startup. Create it now; DB secrets get created in Phase 5 after RDS exists and you have real connection values.

1. **Console → Secrets Manager → Store a new secret → Other type of secret.**
2. Plaintext, paste a 256-bit random value. Generate one with `openssl rand -base64 48` locally (PowerShell equivalent: `[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(48))`).
3. Secret name: `planner/jwt-secret`.
4. Default encryption (AWS-managed `aws/secretsmanager` key). No rotation for v1.

Secret values are editable later: click the secret → Retrieve secret value → Edit. So if you create extras now and want to update them later, that's fine.

## Phase 4 — ECR repository

1. **Console → ECR → Repositories → Create repository.**
2. Visibility: **Private**.
3. Name: `planner-backend`.
4. **Tag immutability: enabled.** This is important — the deploy workflow re-tags `latest` to point at a SHA tag during rollbacks. Immutability would block that, so… actually, **leave it disabled.** The workflow needs to overwrite the `latest` tag every deploy.
5. Encryption: default (AES-256).
6. Note the repository URI shown after creation — looks like `123456789012.dkr.ecr.us-east-1.amazonaws.com/planner-backend`.

## Phase 5 — RDS Postgres

1. **Console → RDS → Databases → Create database.**
2. **Standard create → PostgreSQL.** Latest version is fine.
3. Templates: **Free tier** if your account is eligible, otherwise **Dev/Test**.
4. **Availability and durability: Single-AZ DB instance deployment (1 instance).** Multi-AZ doubles your monthly cost and isn't Free Tier eligible; the uptime delta (99.95% vs 99.5%) isn't worth it for a personal project. Upgrade if/when traffic justifies it.
5. DB instance identifier: `planner-db`.
6. Master username: `planner`. Master password: generate one and save it — step 14 plugs it into Secrets Manager.
7. **Instance configuration → Burstable classes → `db.t4g.micro`.**
8. Storage: 20 GB gp3, default settings.
9. Connectivity:
   - VPC: default
   - Public access: **No**
   - VPC security group: create new, name `planner-db-sg`
   - Availability Zone: no preference
10. Additional configuration:
    - Initial database name: `planner` (creates the DB on first launch — otherwise you have to connect and `CREATE DATABASE` manually)
    - Backup retention: 7 days
    - Enable Performance Insights: optional, free tier-eligible
11. Create database. Wait ~10 minutes for status `Available`.
12. Once available, click the DB → Connectivity & security → note the **Endpoint** (e.g. `planner-db.abcde.us-east-1.rds.amazonaws.com`).
13. Construct the JDBC URL:
    ```
    jdbc:postgresql://planner-db.abcde.us-east-1.rds.amazonaws.com:5432/planner
    ```
14. **Console → Secrets Manager → Store a new secret.** Create **three separate secrets**, one at a time. For each: Other type of secret → **Plaintext tab** (not Key/value) → paste just the value as a bare string (no quotes, no JSON). One secret per row of the table below:

    | Secret name | Plaintext value |
    |------|-------|
    | `planner/db-url` | The JDBC URL from step 13 |
    | `planner/db-user` | The master username from step 6 |
    | `planner/db-password` | The master password from step 6 |

    **Don't** create a single secret with three key/value pairs — ECS task definitions map one secret to one env var, and the three-separate-plaintext-secrets layout is what Phase 7 wires up.

## Phase 5b — SES for transactional email

The backend sends one transactional email today: the verification link for a
self-service **email change** (Settings → Account). In prod (`EMAIL_PROVIDER=ses`)
this goes through AWS SES; in dev the default `log` provider just writes the link
to the application log, so no setup is needed locally.

1. **Console → SES → Configuration → Identities → Create identity.**
   - Verify a **domain** (recommended): SES gives you DKIM CNAME records to add in
     Route 53 (Phase 1). Domain verification + DKIM is what keeps mail out of spam.
   - A single verified email address also works for a first cut, but domain
     verification is the production-grade choice.
2. Set `EMAIL_FROM` (Phase 7, step 5) to an address on that verified identity.
3. **Move out of the SES sandbox.** New SES accounts can only send to *verified*
   recipients. To email arbitrary users, request production access:
   **SES → Account dashboard → Request production access.** This is a manual AWS
   review and can take a day — do it early.
4. **Grant the ECS task role permission to send.** Add an inline policy to the
   task role allowing `ses:SendEmail` (and `ses:SendRawEmail`) on `*` (or scope to
   the verified identity ARN). Credentials resolve from the task role via the
   default AWS provider chain — no keys in env vars.

If you skip this phase, set `EMAIL_PROVIDER=log` in Phase 7 to keep the app
healthy; the email-change feature will then log links instead of sending them.

## Phase 6 — S3 + CloudFront for the frontend

### 6a. S3 bucket

1. **Console → S3 → Create bucket.**
2. Bucket name: `echelplanner-frontend-prod` (must be globally unique; if taken, append a suffix and update the policies above).
3. Region: us-east-1.
4. **Block all public access: keep enabled.** CloudFront will access via OAC, not public reads.
5. Bucket versioning: enabled (gives you a rollback safety net for accidental `--delete` syncs).
6. Default encryption: SSE-S3 (default).
7. Create bucket.

### 6b. ACM certificate

1. **Console → Certificate Manager → us-east-1 region → Request → Request a public certificate.**
2. Domain names:
   - `echelplanner.com`
   - `www.echelplanner.com`
   - `api.echelplanner.com`
3. Validation method: **DNS validation**.
4. After requesting, click the cert → for each domain, click "Create record in Route 53." That auto-adds the validation `CNAME`. Wait ~5 min for status `Issued`.

### 6c. CloudFront distribution

AWS reorganized the CloudFront console into a multi-step wizard in 2025. Step names below match what the console shows. AWS iterates this UI frequently — if your screen doesn't match, the choices below should still be findable.

1. **Console → CloudFront → Create distribution.**
2. **Specify origin:**
   - Origin type: **Amazon S3**
   - S3 origin: pick the bucket from 6a
   - Origin path: leave empty
3. **Settings** (same page):
   - **Allow private S3 bucket access to CloudFront** (Recommended): ✓ keep checked. CloudFront auto-creates an Origin Access Control and updates the S3 bucket policy for you — no manual policy paste needed.
   - **Use recommended origin settings**: ✓ keep selected
   - **Use recommended cache settings tailored to serving S3 content**: ✓ keep selected (applies CachingOptimized)
4. **Enable security** (next page — WAF):
   - **Web Application Firewall (WAF)**: leave the basic protections enabled. AWS bundles these with CloudFront at no extra charge as of 2025.
   - **Use monitor mode**: unchecked (monitor mode is for tuning custom rules; AWS-managed rules are pre-tuned).
   - **Protection against Layer 7 DDoS attacks**: off (paid Business plan only).
5. **Specify domains, certificate, and other settings** (next page):
   - Alternate domain names (CNAMEs): `echelplanner.com`, `www.echelplanner.com`
   - Custom SSL certificate: pick the ACM cert from 6b
   - Default root object: `index.html`
   - Price class: **Use only North America and Europe** (cheaper; expand later if needed)
6. Create distribution. Note the distribution ID (e.g. `E1ABCDEF234567`) and its CloudFront domain name (e.g. `d1xxx.cloudfront.net`).
7. **SPA fallback.** Distribution → Error pages → Create custom error response twice:
   - 403 → Response page path `/index.html` → HTTP response code `200`
   - 404 → Response page path `/index.html` → HTTP response code `200`
   This makes deep links (e.g. `/projects/42`) load the SPA shell.

### 6d. Route 53 records for the frontend

1. **Console → Route 53 → Hosted zones → echelplanner.com → Create record.**
2. Two records, both **Alias** type, both pointing at the CloudFront distribution:
   - Record name: empty (apex) → Route traffic to: Alias to CloudFront distribution → pick the one from 6c
   - Record name: `www` → same alias target
3. Save. DNS usually resolves within a minute, occasionally up to 15.

## Phase 7 — ECS Express Mode service

> **Note on accuracy.** AWS deprecated App Runner to new customers on April 30, 2026 and recommends ECS Express Mode (launched re:Invent 2024) as the replacement. ECS Express is newer than App Runner, and the console wizard is still evolving. The steps below describe the intended architecture; if the wizard UI you see differs, fall back to the architectural intent (one Fargate service, one ALB, ACM cert for HTTPS, custom domain via Route 53 ALIAS) and consult the [ECS Express Mode docs](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/express-mode.html) for current step names.

The resulting resources are managed by standard ECS APIs (cluster, service, task definition, ALB), which is what the CI workflow uses for deploys.

### 7a. Create the service

1. **Console → Amazon ECS → Express deploy** (or "Create cluster" → Express mode, depending on console version).
2. Application details:
   - Application name: `planner-backend`
   - Container image URI: `<account>.dkr.ecr.us-east-1.amazonaws.com/planner-backend:latest`
   - Container port: `8080`
3. Compute:
   - Launch type: **Fargate** (Express mode default)
   - CPU: **1 vCPU**, Memory: **2 GB**
4. Networking:
   - Use the **default VPC** so the service can reach the RDS instance in that VPC.
   - Public endpoint: enabled (ALB with public IPs).
5. Environment variables (plain values):
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `APP_CORS_ALLOWED_ORIGINS` = `https://echelplanner.com,https://www.echelplanner.com`
   - `FRONTEND_BASE_URL` = `https://echelplanner.com` — the public origin email-change verification links point back to (the frontend `/verify-email` route).
   - `EMAIL_PROVIDER` = `ses` (the prod default; set `log` to disable real sending and just log the link).
   - `EMAIL_FROM` = a **verified SES sender identity** (e.g. `no-reply@echelplanner.com`). See [Phase 5b](#phase-5b--ses-for-transactional-email).
   - `EMAIL_SES_REGION` = the SES region (e.g. `us-east-1`) if it differs from the task's default AWS region; otherwise omit.
6. Environment variables (from Secrets Manager — the wizard usually labels these "Secrets" or "Sensitive data"). **Paste the full ARN, not the bare name** — ECS will reject bare names with a misleading "Systems Manager parameter name is invalid" error:
   - `JWT_SECRET` ← full ARN of `planner/jwt-secret` (Secrets Manager → click the secret → copy the Secret ARN at the top)
   - `PLANNER_DB_URL` ← full ARN of `planner/db-url`
   - `PLANNER_DB_USER` ← full ARN of `planner/db-user`
   - `PLANNER_DB_PASSWORD` ← full ARN of `planner/db-password`

   The ARN includes a 6-character random suffix after the secret name (e.g. `arn:aws:secretsmanager:us-east-1:<account>:secret:planner/jwt-secret-AbC123`). You can't construct it from just the name — copy it from the console.
7. Health check (configured on the ALB target group): HTTP path `/health` on port 8080, healthy threshold 2, interval 30s. (See [Health endpoints](#health-endpoints) below — use `/health` not `/actuator/health`; the latter lives on the management port and isn't reachable via the ALB.)
8. Create. The wizard will provision: an ECS cluster, a task definition, a Fargate service, an ALB with target group, and security groups. Wait 5–10 min for the service to reach `RUNNING` with `desired count` matching `running count`.

### 7b. Connect ECS to RDS

The default RDS security group `planner-db-sg` (created in Phase 5) only allows traffic from inside its own security group. ECS Express runs in your default VPC, but its tasks have a different security group.

1. **EC2 → Security groups → planner-db-sg → Inbound rules → Edit.**
2. Add a rule: PostgreSQL (5432) from the ECS service's security group. To find it: ECS → cluster → service → Networking tab → Security groups. Copy the SG ID and paste it as the source.
3. Save. The backend will reconnect on the next health-check cycle.

### 7c. HTTPS on the ALB

The ECS Express wizard provisions an ALB with HTTP (port 80) by default. We need HTTPS on port 443 using the ACM cert from 6b.

1. **EC2 → Load Balancers → pick the ALB the wizard created** (name will include `planner-backend`).
2. **Listeners and rules → Add listener.**
3. Protocol: HTTPS, Port: 443.
4. Default SSL/TLS certificate: pick the ACM cert from 6b (`echelplanner.com` with SANs for `www` and `api`).
5. Default action: Forward to the same target group as the existing port-80 listener.
6. Add the listener. Optionally, edit the port-80 listener to redirect to HTTPS.

### 7d. Custom domain

1. **Route 53 → Hosted zones → echelplanner.com → Create record.**
2. Record name: `api`. Type: **A** with **Alias** toggled on. Route traffic to: Alias to Application and Classic Load Balancer → us-east-1 → pick the ALB from 7c.
3. Save. `https://api.echelplanner.com/health` should return `{"status":"UP"}` within a minute or two.

### 7e. Note the deploy targets

The CI workflow needs to know which service to redeploy on each `main` push. Find:
- **ECS cluster name** — Console → ECS → Clusters → your cluster's name (e.g. `planner-backend-cluster`)
- **ECS service name** — inside that cluster, your service's name (e.g. `planner-backend-service`)

Both go into GitHub Variables in Phase 8.

## Phase 8 — Wire GitHub Variables

**GitHub → repo Settings → Secrets and variables → Actions → Variables tab → New repository variable.** Add:

| Name | Value |
|------|-------|
| `AWS_REGION` | `us-east-1` |
| `ECR_REPOSITORY` | `planner-backend` |
| `ECS_CLUSTER` | ECS cluster name from 7e (e.g. `planner-backend-cluster`) |
| `ECS_SERVICE` | ECS service name from 7e (e.g. `planner-backend-service`) |
| `S3_BUCKET` | `echelplanner-frontend-prod` |
| `CLOUDFRONT_DISTRIBUTION_ID` | distribution ID from 6c |
| `BACKEND_URL` | `https://api.echelplanner.com` |
| `FRONTEND_URL` | `https://echelplanner.com` |

## Phase 9 — First deploy

1. Merge this PR's branch into `dev`.
2. Open a PR from `dev` to `main`. Merge it.
3. The push to `main` triggers `.github/workflows/deploy.yml`. Watch the run in the Actions tab.
4. On success, both `https://echelplanner.com` and `https://api.echelplanner.com/health` should return 200.

Something will almost certainly go wrong on the first deploy — usually an IAM permission gap or an env var name mismatch. Read the failing step's logs; the most common fixes:
- ECS task can't pull from ECR → the ECS task execution role needs `AmazonECSTaskExecutionRolePolicy` (Express mode usually attaches this automatically)
- ECS task crash-looping → check the task's stopped reason and CloudWatch logs (set in Express mode by default); usually missing env var or DB unreachable
- DB connection refused → re-check the `planner-db-sg` inbound rule allows the ECS service's security group on 5432
- Frontend 403 → S3 bucket policy doesn't grant CloudFront OAC access
- Frontend loads but API calls 404 → confirm `VITE_API_URL` was set during the build (check the workflow run logs)

---

## Env var inventory

What's stored where:

| Variable | Where it lives | Who reads it |
|----------|----------------|--------------|
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | GitHub repo Secrets | The deploy workflow |
| `AWS_REGION`, `ECR_REPOSITORY`, ... | GitHub repo Variables | The deploy workflow |
| `JWT_SECRET`, `PLANNER_DB_URL`, `PLANNER_DB_USER`, `PLANNER_DB_PASSWORD` | AWS Secrets Manager | The backend container at startup |
| `SPRING_PROFILES_ACTIVE=prod`, `APP_CORS_ALLOWED_ORIGINS` | ECS task definition env vars | The backend container at startup |
| `FRONTEND_BASE_URL`, `EMAIL_PROVIDER`, `EMAIL_FROM`, `EMAIL_SES_REGION` | ECS task definition env vars | The backend container at startup (email-change verification) |
| `VITE_API_URL` | GitHub repo Variable (`BACKEND_URL`), inlined into the build | Baked into the SPA bundle at build time |

## Rollback procedure

The workflow supports rollback as a manual dispatch.

1. **Actions tab → Deploy → Run workflow.**
2. Enter the SHA of the commit you want to roll back to (e.g. `065e293c14...` — full or 7+ chars).
3. The workflow:
   - Looks up that SHA's image in ECR (must have been pushed by a prior deploy)
   - Re-tags it as `:latest`
   - Triggers the ECS service to redeploy via `aws ecs update-service --force-new-deployment`
   - Re-runs the smoke test
4. The frontend is rolled back by re-running the workflow from the `main` ref that points at that SHA. If you've already merged the bad change forward, the cleanest path is `git revert` the bad commit on `main`, which triggers a forward-fix deploy with the prior bundle restored.

### Limits of rollback

- **Flyway is forward-only.** If the bad version applied a destructive migration (dropped a column, narrowed a constraint), rolling back the application code will leave the schema in the migrated state — and the rolled-back code may not work against it. Schema reverts are a manual, planned operation, not part of this workflow.
- **ECS does rolling deploys by default.** Briefly during a deploy, both the old and new task revisions handle traffic. Avoid this overlap for destructive migrations: temporarily set the service's deployment configuration to "minimum healthy percent = 0, maximum percent = 100" so the old task drains fully before the new one starts. Better fix is to make migrations strictly additive — see [Database migration safety](#database-migration-safety) below.

## Database migration safety

Flyway runs on app startup. ECS rolling deploys mean two app versions briefly coexist. To stay safe:

1. **Make migrations strictly additive** when at all possible. Add columns as nullable, never drop, never narrow types. The old version keeps running against the new schema.
2. **If a destructive migration is unavoidable:**
   - Update the service's deployment configuration (minimum healthy percent = 0) so the old task drains before the new one starts.
   - Or: scale the service desired count to 0, run the migration manually against RDS (`psql` from a bastion or a one-shot ECS task), then scale back up with the new image.
   - Or: split into two deploys — one to write to both old and new columns, then one to drop the old column after backfill.

This is annoying enough that we should never reach for it casually. When in doubt, add a new column, leave the old alone, and clean up months later.

## Self-host fallback (Docker Compose)

For local prod-like testing or running on your own server, `compose.deploy.yml` ships the backend + Postgres in containers. The operator is responsible for putting a TLS-terminating reverse proxy (Caddy, nginx, or Traefik) in front of the backend and forwarding `X-Forwarded-Proto: https`. We do not ship a proxy.

```bash
docker compose -f compose.deploy.yml up -d --build
curl http://localhost:8080/actuator/health
```

Override defaults via env vars (see the comments at the top of the compose file).

## TLS termination

Both deploy shapes terminate TLS upstream of the Spring Boot app. The prod profile sets:

```properties
server.forward-headers-strategy=framework
```

This tells Spring to honor `X-Forwarded-Proto` from the proxy / ALB, so `HttpServletRequest.isSecure()` returns `true` for forwarded HTTPS requests.

**Why this matters.** Refresh-token cookies are issued with `Secure; HttpOnly; SameSite=Strict`. The `Secure` attribute tells the browser to send the cookie only over HTTPS. If Spring sees the inbound request as plaintext HTTP (because forward headers are not honored), it will issue cookies under a non-secure context, browsers will drop the cookie at the next round-trip, and authentication silently breaks. Server logs show successful 200s; only the client-side missing cookie reveals the problem.

**Cross-origin cookie note.** In the AWS shape, frontend (`echelplanner.com`) and backend (`api.echelplanner.com`) are different origins but share a registrable domain — the browser treats fetches between them as "same-site." `SameSite=Strict` cookies are sent on these requests, so refresh works without changing the cookie attribute.

## Startup guard

`HttpsTerminationGuard` (in `backend/src/main/java/com/echel/planner/backend/common/`) runs under the `prod` profile only. It fails application startup with `IllegalStateException` unless one of the following is configured:

- `server.forward-headers-strategy` is set (typical for ALB / nginx / Caddy / Traefik), **or**
- `server.ssl.enabled=true` (Spring terminating TLS itself — uncommon for this app but allowed).

If neither is set, the app refuses to start rather than running silently misconfigured.

Companion guard: `JwtSecretProductionGuard` (see [#79](https://github.com/rlaprelle/Planner/issues/79)) fails startup if the dev-default JWT secret leaks into prod.

## Health endpoints

Two distinct concepts, on two different ports:

| Endpoint | Port | Aggregates downstream? | Use case |
|----------|------|------------------------|----------|
| `/health` (HealthController) | 8080 (main) | No — process-up only | ALB target group health check, external smoke test, anything publicly reachable |
| `/actuator/health` | 9090 (management) | Yes — DB, Hikari pool, etc. | Internal observability, deep diagnostics, Prometheus scraper |

The lightweight `/health` endpoint deliberately doesn't touch downstream dependencies: a brief Postgres hiccup shouldn't take the whole service out of the load balancer. The aggregated `/actuator/health` on the management port (9090) is where you go to ask "and is the database also OK?" — but that port isn't exposed via the ALB, so it's only reachable from inside the task's VPC.

When configuring the ALB target group health check, use **path `/health`, port 8080**. Not `/actuator/health`.

## Required env vars under prod

(No localhost / dev defaults — startup fails fast if unset.)

- `SPRING_PROFILES_ACTIVE=prod`
- `JWT_SECRET` — 256-bit secret
- `PLANNER_DB_URL`, `PLANNER_DB_USER`, `PLANNER_DB_PASSWORD`
- `APP_CORS_ALLOWED_ORIGINS` — comma-separated list of allowed origins (default permits localhost dev only)

## Follow-ups

Worth doing eventually, scoped out of [#88](https://github.com/rlaprelle/Planner/issues/88):

- **OIDC instead of access keys** for the GitHub → AWS trust. Removes long-lived credentials from the repo.
- **Drop the ECS SG → DB SG rule** if you eventually move ECS into a private subnet — currently the DB security group must allow the ECS service's SG explicitly.
- **Infrastructure as code** (Terraform or CDK) so a second environment is reproducible. Clickops is fine for the first env; the second one is where you regret it.
- **CloudWatch alarms** on ECS deployment-failure and ALB 5xx rate. Currently the deploy workflow catches startup failures; runtime regressions wait for a user to report.
- **CDN for the API** if global latency becomes a concern. The ALB is regional.
