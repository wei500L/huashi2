#!/usr/bin/env bash
set -euo pipefail
set -a
source /opt/lexibridge/deploy/.env
set +a

docker exec -e MYSQL_PWD="$APP_DB_ROOT_PASSWORD" ef-transfer-mysql \
  mysql -uroot -e "USE \`$APP_DB_NAME\`;
UPDATE ai_async_job
SET status='FAILED',
    error_message='Marked failed: provider hang or subscription cancelled (ops cleanup)',
    finished_at=CURRENT_TIMESTAMP,
    updated_at=CURRENT_TIMESTAMP
WHERE status IN ('PENDING','RUNNING')
  AND created_at < (UTC_TIMESTAMP() - INTERVAL 5 MINUTE);
SELECT job_id, scene, status, left(error_message,80), created_at, finished_at
FROM ai_async_job ORDER BY id DESC LIMIT 10;"
