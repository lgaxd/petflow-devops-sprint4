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

echo "==> 1/4 App Settings do $WEB_APP_NAME (valores não são exibidos)"
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

echo "==> 2/4 Enviando $(basename "$JAR") para o Web App"
az webapp deploy --resource-group "$RESOURCE_GROUP" --name "$WEB_APP_NAME" \
  --src-path "$JAR" --type jar --track-status false --output none

# O "az webapp deploy" volta ANTES de o app reiniciar com o JAR novo. Se o health check rodar nesse intervalo,
# a instância ANTIGA responde 200 e o CD seguiria para o smoke test com o app prestes a reiniciar.
# Por isso: 1) espera o reinício aparecer (o app sai do ar), 2) espera voltar, 3) confirma 3 respostas seguidas.
code() { curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$URL/actuator/health" || true; }

echo "==> 3/4 Aguardando o app reiniciar com o JAR novo (até 90 s)"
SAW_RESTART=0
for i in $(seq 1 45); do
  CODE="$(code)"
  if [ "$CODE" != "200" ]; then SAW_RESTART=1; echo "  reinício detectado (HTTP $CODE)"; break; fi
  sleep 2
done
[ "$SAW_RESTART" = 1 ] || echo "  reinício não observado em 90 s (o app pode já ter reiniciado); seguindo para a verificação de saúde"

echo "==> 4/4 Aguardando $URL/actuator/health responder UP (até 10 minutos)"
for i in $(seq 1 60); do
  CODE="$(code)"
  if [ "$CODE" = "200" ]; then
    # confirma estabilidade: 3 respostas 200 seguidas (evita pegar a instância antiga ou um app ainda subindo)
    OK=1
    for k in 1 2; do sleep 5; [ "$(code)" = "200" ] && OK=$((OK + 1)); done
    if [ "$OK" = 3 ]; then
      echo "Aplicação saudável (aplicação + PostgreSQL): $URL"
      exit 0
    fi
    echo "  instável, aguardando..."
  else
    echo "  tentativa $i/60: HTTP $CODE"
  fi
  sleep 10
done

echo "ERRO: a aplicação não ficou saudável."
echo "  503 = app no ar, mas sem acesso ao PostgreSQL (firewall, DB_URL ou senha errados)."
echo "  000 = app ainda não respondeu. Logs: az webapp log tail -g $RESOURCE_GROUP -n $WEB_APP_NAME"
exit 1
