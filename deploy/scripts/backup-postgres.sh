#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)
DEPLOY_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)
ENV_FILE=${ENV_FILE:-"$DEPLOY_DIR/.env"}

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

BACKUP_DIR=${BACKUP_DIR:-"$DEPLOY_DIR/backups"}
BACKUP_RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-7}
TIMESTAMP=$(date -u +%Y%m%dT%H%M%SZ)
OUTPUT_DIR="$BACKUP_DIR/postgres"
OUTPUT_FILE="$OUTPUT_DIR/${AI_DB_NAME:-ef_transfer_ai}-$TIMESTAMP.dump"

mkdir -p "$OUTPUT_DIR"

docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" exec -T postgres \
  sh -lc 'exec pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB"' \
  > "$OUTPUT_FILE"

find "$OUTPUT_DIR" -type f -name '*.dump' -mtime +"$BACKUP_RETENTION_DAYS" -delete

echo "PostgreSQL backup written to $OUTPUT_FILE"
