#!/usr/bin/env bash
set -euo pipefail
PROD=/opt/lexibridge
cd "$PROD/deploy"
docker compose -f docker-compose.yml -f docker-compose.production.yml --env-file .env build ai-gateway
docker compose -f docker-compose.yml -f docker-compose.production.yml --env-file .env up -d --force-recreate ai-gateway
for i in $(seq 1 24); do
  s=$(docker inspect -f '{{.State.Health.Status}}' ef-transfer-ai-gateway 2>/dev/null || echo starting)
  echo "attempt=$i health=$s"
  if [ "$s" = "healthy" ]; then
    echo AI_GATEWAY_HEALTHY
    exit 0
  fi
  sleep 5
done
docker logs --tail 60 ef-transfer-ai-gateway || true
exit 1
