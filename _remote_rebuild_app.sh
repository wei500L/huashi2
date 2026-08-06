#!/usr/bin/env bash
set -euo pipefail
cd /opt/lexibridge/deploy
docker compose -f docker-compose.yml -f docker-compose.production.yml --env-file .env build app-server
docker compose -f docker-compose.yml -f docker-compose.production.yml --env-file .env up -d --force-recreate app-server
for i in $(seq 1 24); do
  s=$(docker inspect -f '{{.State.Health.Status}}' ef-transfer-app-server 2>/dev/null || echo starting)
  echo "attempt=$i health=$s"
  if [ "$s" = "healthy" ]; then
    echo APP_SERVER_HEALTHY
    exit 0
  fi
  sleep 5
done
docker logs --tail 80 ef-transfer-app-server || true
exit 1
