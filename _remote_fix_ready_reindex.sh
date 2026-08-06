#!/usr/bin/env bash
set -euo pipefail
set -a
source /opt/lexibridge/deploy/.env
set +a

echo "== mark active lexical pairs READY =="
docker exec -e MYSQL_PWD="$APP_DB_ROOT_PASSWORD" ef-transfer-mysql \
  mysql -uroot -e "USE \`$APP_DB_NAME\`;
UPDATE lexical_pair
SET knowledge_status='READY', embedding_status='PENDING', updated_at=CURRENT_TIMESTAMP
WHERE deleted=0 AND active=1 AND (knowledge_status IS NULL OR knowledge_status<>'READY');
SELECT id, english_word, knowledge_status, active FROM lexical_pair WHERE deleted=0 ORDER BY id;"
