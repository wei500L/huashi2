#!/usr/bin/env bash
set -euo pipefail
PROD=/opt/lexibridge
cd "$PROD/deploy"

echo "== docker compose build app-server ai-gateway frontend =="
docker compose \
  -f "$PROD/deploy/docker-compose.yml" \
  -f "$PROD/deploy/docker-compose.production.yml" \
  --env-file "$PROD/deploy/.env" \
  build app-server ai-gateway frontend

echo "== recreate services =="
docker compose \
  -f "$PROD/deploy/docker-compose.yml" \
  -f "$PROD/deploy/docker-compose.production.yml" \
  --env-file "$PROD/deploy/.env" \
  up -d --force-recreate app-server ai-gateway frontend

echo "== wait healthy =="
for i in $(seq 1 90); do
  AS=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' ef-transfer-app-server 2>/dev/null || echo starting)
  AG=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' ef-transfer-ai-gateway 2>/dev/null || echo starting)
  FE=$(docker inspect -f '{{.State.Status}}' ef-transfer-frontend 2>/dev/null || echo starting)
  echo "attempt=$i app-server=$AS ai-gateway=$AG frontend=$FE"
  if [ "$AS" = "healthy" ] && [ "$AG" = "healthy" ] && [ "$FE" = "running" ]; then
    echo "DEPLOY_HEALTHY"
    docker ps --filter name=ef-transfer --format '{{.Names}} {{.Status}}'
    exit 0
  fi
  # surface crash logs early
  if [ "$AS" = "unhealthy" ] || [ "$AS" = "exited" ]; then
    docker logs --tail 80 ef-transfer-app-server || true
  fi
  if [ "$AG" = "unhealthy" ] || [ "$AG" = "exited" ]; then
    docker logs --tail 80 ef-transfer-ai-gateway || true
  fi
  sleep 10
done
echo "DEPLOY_TIMEOUT"
docker ps --filter name=ef-transfer --format '{{.Names}} {{.Status}}'
docker logs --tail 100 ef-transfer-app-server || true
docker logs --tail 100 ef-transfer-ai-gateway || true
exit 1
