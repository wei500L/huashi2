#!/usr/bin/env bash
set -euo pipefail

PROD=/opt/lexibridge
cd "$PROD/deploy"

set -a
# shellcheck disable=SC1091
source "$PROD/deploy/.env"
set +a

echo "== create ai_async_job if missing =="
docker exec -i ef-transfer-mysql mysql -uroot -p"$APP_DB_ROOT_PASSWORD" "$APP_DB_NAME" <<'SQL'
CREATE TABLE IF NOT EXISTS `ai_async_job` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_id` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `scene` varchar(64) NOT NULL,
  `status` varchar(32) NOT NULL,
  `request_json` longtext NOT NULL,
  `result_json` longtext,
  `error_message` varchar(1000) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `started_at` timestamp NULL DEFAULT NULL,
  `finished_at` timestamp NULL DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_async_job_job_id` (`job_id`),
  KEY `idx_ai_async_job_user_created` (`user_id`,`created_at`),
  KEY `idx_ai_async_job_status_created` (`status`,`created_at`),
  CONSTRAINT `fk_ai_async_job_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
);
SQL

TABLE_CHECK=$(docker exec ef-transfer-mysql mysql -uroot -p"$APP_DB_ROOT_PASSWORD" -N -e "USE \`$APP_DB_NAME\`; SHOW TABLES LIKE 'ai_async_job';")
echo "table=$TABLE_CHECK"
test "$TABLE_CHECK" = "ai_async_job"
echo "SCHEMA_OK"

ENV_FILE="$PROD/deploy/.env"
if grep -q '^RAG_RECALL_THRESHOLD=' "$ENV_FILE"; then
  sed -i 's/^RAG_RECALL_THRESHOLD=.*/RAG_RECALL_THRESHOLD=0.35/' "$ENV_FILE"
else
  echo 'RAG_RECALL_THRESHOLD=0.35' >> "$ENV_FILE"
fi
if grep -q '^AI_PROVIDER_PROBE_INTERVAL=' "$ENV_FILE"; then
  sed -i 's/^AI_PROVIDER_PROBE_INTERVAL=.*/AI_PROVIDER_PROBE_INTERVAL=PT5M/' "$ENV_FILE"
else
  echo 'AI_PROVIDER_PROBE_INTERVAL=PT5M' >> "$ENV_FILE"
fi
grep -E '^(RAG_RECALL_THRESHOLD|AI_PROVIDER_PROBE_INTERVAL)=' "$ENV_FILE"

echo "== docker compose build app-server ai-gateway frontend =="
docker compose \
  -f "$PROD/deploy/docker-compose.yml" \
  -f "$PROD/deploy/docker-compose.production.yml" \
  --env-file "$PROD/deploy/.env" \
  build app-server ai-gateway frontend

echo "== recreate services =="
docker compose \
  -f "$PROD/deploy/docker-compose.yml" \
  -f "$PROD/deploy/docker-compose.production.yml" \
  --env-file "$PROD/deploy/.env" \
  up -d --force-recreate app-server ai-gateway frontend

echo "== wait healthy =="
for i in $(seq 1 60); do
  AS=$(docker inspect -f '{{.State.Health.Status}}' ef-transfer-app-server 2>/dev/null || echo starting)
  AG=$(docker inspect -f '{{.State.Health.Status}}' ef-transfer-ai-gateway 2>/dev/null || echo starting)
  FE=$(docker inspect -f '{{.State.Status}}' ef-transfer-frontend 2>/dev/null || echo starting)
  echo "attempt=$i app-server=$AS ai-gateway=$AG frontend=$FE"
  if [ "$AS" = "healthy" ] && [ "$AG" = "healthy" ] && [ "$FE" = "running" ]; then
    echo "DEPLOY_HEALTHY"
    exit 0
  fi
  sleep 10
done
echo "DEPLOY_TIMEOUT"
docker ps --filter name=ef-transfer --format '{{.Names}} {{.Status}}'
exit 1
