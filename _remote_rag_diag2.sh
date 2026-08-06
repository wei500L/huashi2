#!/usr/bin/env bash
set -euo pipefail
set -a
# shellcheck disable=SC1091
source /opt/lexibridge/deploy/.env
set +a

docker exec -e PGPASSWORD="$AI_DB_PASSWORD" ef-transfer-postgres \
  psql -U "$AI_DB_USERNAME" -d "$AI_DB_NAME" <<'SQL'
\x on
SELECT id, source_type, source_id, left(title,100) AS title, left(content,240) AS content_preview, left(snippet,120) AS snippet
FROM knowledge_chunk
WHERE source_type = 'LEXICAL_PAIR'
ORDER BY id;
\x off
SELECT column_name FROM information_schema.columns WHERE table_name='knowledge_chunk' ORDER BY ordinal_position;
SQL

echo "== sample internal retrieve via app-server logs =="
docker logs --since 45m ef-transfer-ai-gateway 2>&1 | grep -E 'rag_retrieve_completed|knowledge_search_|event=knowledge' | tail -40
