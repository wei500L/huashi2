#!/usr/bin/env bash
set -euo pipefail
set -a
# shellcheck disable=SC1091
source /opt/lexibridge/deploy/.env
set +a

echo "== postgres chunk counts =="
docker exec -e PGPASSWORD="$AI_DB_PASSWORD" ef-transfer-postgres \
  psql -U "$AI_DB_USERNAME" -d "$AI_DB_NAME" -c \
  "SELECT count(*) AS docs FROM knowledge_document;
   SELECT count(*) AS chunks FROM knowledge_chunk;
   SELECT source_type, count(*) AS n FROM knowledge_chunk GROUP BY 1 ORDER BY n DESC;
   SELECT embedding_model, count(*) AS n FROM knowledge_chunk GROUP BY 1;
   SELECT left(title,80) AS title, source_type, source_id FROM knowledge_chunk WHERE source_type IN ('LEXICAL_PAIR','LEXICAL_SENSE','LEXICAL_EXAMPLE') ORDER BY id DESC LIMIT 15;"

echo "== recent ai-gateway retrieve logs =="
docker logs --since 30m ef-transfer-ai-gateway 2>&1 | grep -E 'rag_retrieve_completed|knowledge_search' | tail -30

echo "== container health =="
docker ps --filter name=ef-transfer --format '{{.Names}} {{.Status}}'
