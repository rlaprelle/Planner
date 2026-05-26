# Database Backup + Restore Runbook

Procedure for backing up and restoring Echel Planner's PostgreSQL database. Schema evolution is handled by Flyway; this runbook covers data preservation.

## Scope

- **In scope:** PostgreSQL data (`planner` database).
- **Out of scope:** Application code (in git), Flyway migrations (in git).
- **Forward-looking warning:** No user-uploaded files exist today. If avatars, attachments, or other binary user content are introduced later, they will need their own backup story — `pg_dump` and RDS snapshots do not cover object storage (S3, local volumes, etc.).

## Choice of mechanism

Pick the row matching your deploy target.

### Local Docker Compose (`compose.deploy.yml`, per issue #92)

Use `pg_dump` against the running container. Run from the host on a schedule (cron, Task Scheduler, or a sibling container).

```bash
# Daily backup — compressed custom format, includes schema + data
docker exec planner-db pg_dump -U planner -d planner -F c \
  > "backups/planner-$(date +%Y%m%d).dump"
```

Schedule via host cron (Linux/macOS) or Task Scheduler (Windows). Store backups outside the Docker volume so a `docker compose down -v` does not destroy them.

### AWS App Runner + RDS PostgreSQL (per issue #88)

Use **RDS automated backups**. No app-side action needed.

- Enable in console: RDS → Databases → modify instance → **Backup retention period** ≥ 7 days.
- Or Terraform: `backup_retention_period = 7`, `backup_window = "07:00-09:00"`, `copy_tags_to_snapshot = true`.
- This provides daily snapshots plus point-in-time recovery (PITR) to any second within the retention window.

### AWS App Runner + Aurora Serverless v2 (alternative per issue #88)

Use **Aurora continuous backup**. Same posture as RDS, different config surface.

- Console: Aurora → cluster → modify → **Backup retention period** ≥ 7 days.
- Or Terraform on `aws_rds_cluster`: `backup_retention_period = 7`, `preferred_backup_window = "07:00-09:00"`.
- PITR is on by default for Aurora.

## Retention policy

Recommended: **7 daily, 4 weekly, 12 monthly**.

- Docker Compose: implement via a rotation script (e.g., `find backups/ -name 'planner-*.dump' -mtime +7 -delete` for the daily tier; promote one per week and one per month to longer-lived directories).
- RDS / Aurora: set retention period to 7 days for automated snapshots; use the AWS Backup service or a Lambda + EventBridge rule to copy weekly/monthly snapshots into longer retention.

**Tradeoff:** longer retention = more storage cost. Monthly snapshots over 12 months are cheap relative to RDS instance cost; daily snapshots beyond 30 days are usually not worth it.

## Restore procedure

### Docker Compose

```bash
# 1. Stop the app (backend will fail open connections during restore)
docker compose -f compose.deploy.yml stop backend

# 2. Drop and recreate the database
docker exec planner-db psql -U planner -d postgres \
  -c "DROP DATABASE planner;" -c "CREATE DATABASE planner OWNER planner;"

# 3. Restore from dump
docker exec -i planner-db pg_restore -U planner -d planner --no-owner \
  < backups/planner-YYYYMMDD.dump

# 4. Restart the app
docker compose -f compose.deploy.yml start backend
```

### RDS / Aurora

Console: RDS → Snapshots → select snapshot → **Restore snapshot**. This creates a **new instance with a new endpoint** — the app must be reconfigured (`PLANNER_DB_URL`) to point at it. For PITR, use **Restore to point in time** instead of a named snapshot.

AWS CLI equivalent:
```bash
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier planner-restored \
  --db-snapshot-identifier <snapshot-id>
```

After restore, update App Runner's `PLANNER_DB_URL` env var and redeploy.

### End-to-end restore test cadence

A backup that has never been restored is not a backup. **Run a full restore test at least once per quarter.**

Procedure:
1. Spin up a throwaway environment (separate Docker compose project, or a `planner-restore-test` RDS instance).
2. Restore the most recent production backup into it using the procedure above.
3. Point a local backend at the restored DB and verify: login works, task list loads, recent tasks are present.
4. Record the test date and outcome in a changelog (issue comment, ops log, or similar).
5. Tear down the throwaway environment.

If step 3 fails, treat it as a Sev-1 — the backup pipeline is broken and production has no recovery path.

## Pre-launch checklist

- [ ] Backup mechanism configured for chosen deploy target
- [ ] First test restore performed end-to-end
- [ ] Retention policy set
- [ ] Monitoring/alerting on backup job failures (CloudWatch alarm on RDS `BackupRetentionPeriodStorageUsed`, or a cron-job wrapper that pages on non-zero exit)
