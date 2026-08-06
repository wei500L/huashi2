#!/usr/bin/env bash
set -euo pipefail
set -a
# shellcheck disable=SC1091
source /opt/lexibridge/deploy/.env
set +a

docker exec -e PGPASSWORD="$AI_DB_PASSWORD" ef-transfer-postgres \
  psql -U "$AI_DB_USERNAME" -d "$AI_DB_NAME" -c \
  "SELECT id, source_type, source_id, title, left(content,200) AS content FROM knowledge_chunk WHERE source_type='LEXICAL_PAIR' ORDER BY id;"

docker exec -e PGPASSWORD="$AI_DB_PASSWORD" ef-transfer-postgres \
  psql -U "$AI_DB_USERNAME" -d "$AI_DB_NAME" -c \
  "SELECT id, source_type, title FROM knowledge_chunk WHERE content ILIKE '%coin%' OR title ILIKE '%coin%' ORDER BY id;"

docker exec -e PGPASSWORD="$AI_DB_PASSWORD" ef-transfer-postgres \
  psql -U "$AI_DB_USERNAME" -d "$AI_DB_NAME" -c \
  "SELECT count(*) FILTER (WHERE embedding IS NULL) AS null_emb, count(*) AS total FROM knowledge_chunk WHERE source_type='LEXICAL_PAIR';"

# show columns
docker exec -e PGPASSWORD="$AI_DB_PASSWORD" ef-transfer-postgres \
  psql -U "$AI_DB_USERNAME" -d "$AI_DB_NAME" -c \
  "SELECT column_name, data_type FROM information_schema.columns WHERE table_name='knowledge_chunk' ORDER BY ordinal_position;"
