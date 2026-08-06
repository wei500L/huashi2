#!/usr/bin/env bash
set -euo pipefail
set -a
source /opt/lexibridge/deploy/.env
set +a

echo "== MySQL lexical_pair status =="
docker exec -e MYSQL_PWD="$APP_DB_ROOT_PASSWORD" ef-transfer-mysql \
  mysql -uroot -N -e "USE \`$APP_DB_NAME\`; SELECT id, english_word, french_word, active, knowledge_status, embedding_status FROM lexical_pair WHERE deleted=0 ORDER BY id LIMIT 20;"

echo "== Postgres chunk status =="
docker exec -e PGPASSWORD="$AI_DB_PASSWORD" ef-transfer-postgres \
  psql -U "$AI_DB_USERNAME" -d "$AI_DB_NAME" -c \
  "SELECT kc.id, kc.active, kc.embedding_status, kd.active AS doc_active, kc.title
   FROM knowledge_chunk kc JOIN knowledge_document kd ON kd.id=kc.document_id
   WHERE kc.source_type='LEXICAL_PAIR' ORDER BY kc.id;"
