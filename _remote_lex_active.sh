#!/usr/bin/env bash
set -euo pipefail
set -a
source /opt/lexibridge/deploy/.env
set +a
docker exec -e PGPASSWORD="$AI_DB_PASSWORD" ef-transfer-postgres \
  psql -U "$AI_DB_USERNAME" -d "$AI_DB_NAME" -c \
  "SELECT kc.id, kc.active AS chunk_active, kd.active AS doc_active, kc.embedding_status, left(kc.title,40)
   FROM knowledge_chunk kc JOIN knowledge_document kd ON kd.id=kc.document_id
   WHERE kc.source_type='LEXICAL_PAIR';"
docker exec -e PGPASSWORD="$AI_DB_PASSWORD" ef-transfer-postgres \
  psql -U "$AI_DB_USERNAME" -d "$AI_DB_NAME" -c \
  "SELECT count(*) FROM knowledge_chunk kc JOIN knowledge_document kd ON kd.id=kc.document_id
   WHERE kc.active AND kd.active AND (lower(kc.title) LIKE '%coin%' OR lower(kc.content) LIKE '%coin%');"
