#!/usr/bin/env bash
# Deploy do JAR no Azure Web App + espera da aplicação (e do PostgreSQL) ficarem saudáveis.
# Usado pelo CD (AzureCLI@2). Também funciona manualmente:
#   bash scripts/deploy-app.sh target/petflow-api.jar
# Segredos (DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET) chegam como variáveis de
# ambiente (Variable Group no CD, .env.azure no uso local). Nunca ficam no código.
set -euo pipefail

if [ -f "$(dirname "$0")/../.env.azure" ]; then set -a; source <(tr -d '\r' < "$(dirname "$0")/../.env.azure"); set +a; fi
: "${RESOURCE_GROUP:?defina RESOURCE_GROUP}" "${WEB_APP_NAME:?defina WEB_APP_NAME}"
: "${DB_URL:?defina DB_URL}" "${DB_USERNAME:?defina DB_USERNAME}" "${DB_PASSWORD:?defina DB_PASSWORD}" "${JWT_SECRET:?defina JWT_SECRET}"

JAR="${1:-$(dirname "$0")/../target/petflow-api.jar}"
[ -f "$JAR" ] || { echo "ERRO: JAR não encontrado: $JAR"; exit 1; }

HOST="$(az webapp show --resource-group "$RESOURCE_GROUP" --name "$WEB_APP_NAME" --query defaultHostName --output tsv)"
URL="https://$HOST"

echo "==> 1/3 App Settings do $WEB_APP_NAME (valores não são exibidos)"
SETTINGS_FILE="$(mktemp)"
trap 'rm -f "$SETTINGS_FILE"' EXIT
chmod 600 "$SETTINGS_FILE"
URL="$URL" python3 - "$SETTINGS_FILE" <<'PY'
import json, os, sys
e = os.environ
json.dump({
    "DB_URL": e["DB_URL"],
    "DB_USERNAME": e["DB_USERNAME"],
    "DB_PASSWORD": e["DB_PASSWORD"],
    "JWT_SECRET": e["JWT_SECRET"],
    "CORS_ALLOWED_ORIGINS": e["URL"],      # o Swagger é servido pela própria API
    "WEBSITES_PORT": "8080",
    "WEBSITES_CONTAINER_START_TIME_LIMIT": "600",
}, open(sys.argv[1], "w"))
PY
az webapp config appsettings set --resource-group "$RESOURCE_GROUP" --name "$WEB_APP_NAME" \
  --settings "@$SETTINGS_FILE" --output none

echo "==> 2/3 Enviando $(basename "$JAR") para o Web App"
az webapp deploy --resource-group "$RESOURCE_GROUP" --name "$WEB_APP_NAME" \
  --src-path "$JAR" --type jar --track-status false --output none

echo "==> 3/3 Aguardando $URL/actuator/health responder UP (até 10 minutos)"
for i in $(seq 1 60); do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$URL/actuator/health" || true)
  if [ "$CODE" = "200" ]; then
    echo "Aplicação saudável (aplicação + PostgreSQL): $URL"
    exit 0
  fi
  echo "  tentativa $i/60: HTTP $CODE"
  sleep 10
done

echo "ERRO: a aplicação não ficou saudável."
echo "  503 = app no ar, mas sem acesso ao PostgreSQL (firewall, DB_URL ou senha errados)."
echo "  000 = app ainda não respondeu. Logs: az webapp log tail -g $RESOURCE_GROUP -n $WEB_APP_NAME"
exit 1
