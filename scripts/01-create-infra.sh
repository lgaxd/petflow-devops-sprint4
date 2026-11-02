#!/usr/bin/env bash
# Cria (ou reaproveita) toda a infraestrutura do PetFlow Sprint 4 na Azure:
#   Resource Group -> Azure Database for PostgreSQL Flexible Server -> App Service Plan B1 -> Web App Java 21
# Nomes novos e separados da infra da disciplina de Java (561413-petflow-rg etc.).
# O banco é criado AQUI, uma única vez; o CD só publica o JAR. O armazenamento do PostgreSQL é gerenciado
# pela Azure (persistente, com backup), então os dados sobrevivem a reinícios.
# Uso: bash scripts/01-create-infra.sh      (precisa de 'az login' e do arquivo .env.azure)
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_vars LOCATION RESOURCE_GROUP APP_SERVICE_PLAN WEB_APP_NAME PG_SERVER_NAME PG_DATABASE \
             PG_ADMIN_USER PG_ADMIN_PASSWORD DB_USERNAME DB_PASSWORD JWT_SECRET

command -v psql >/dev/null || { echo "ERRO: psql não encontrado. Instale: sudo apt update && sudo apt install -y postgresql-client"; exit 1; }
echo "==> Assinatura Azure em uso"
require_az
az provider register --namespace Microsoft.DBforPostgreSQL --wait --output none

echo "==> 1/6 Resource Group $RESOURCE_GROUP ($LOCATION)"
RG_LOCATION="$(az group show --name "$RESOURCE_GROUP" --query location --output tsv 2>/dev/null || true)"
if [ -z "$RG_LOCATION" ]; then
  az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none
elif [ "$RG_LOCATION" != "$LOCATION" ]; then
  echo "    AVISO: o Resource Group já existe em $RG_LOCATION (os recursos abaixo irão para $LOCATION)."
  echo "    Para ficar tudo na mesma região, apague o grupo vazio e rode de novo:"
  echo "      az group delete -n $RESOURCE_GROUP --yes"
fi

echo "==> 2/6 PostgreSQL Flexible Server $PG_SERVER_NAME (Burstable B1ms, 32 GB, PostgreSQL 16)"
if az postgres flexible-server show -g "$RESOURCE_GROUP" -n "$PG_SERVER_NAME" --output none 2>/dev/null; then
  echo "    já existe, reaproveitando"
else
  # --public-access 0.0.0.0 cria a regra "Allow Azure services" (o Web App acessa por aí).
  az postgres flexible-server create -g "$RESOURCE_GROUP" -n "$PG_SERVER_NAME" -l "$LOCATION" \
    --tier Burstable --sku-name Standard_B1ms --storage-size 32 --version 16 \
    --admin-user "$PG_ADMIN_USER" --admin-password "$PG_ADMIN_PASSWORD" \
    --public-access 0.0.0.0 --yes --output none
fi
# O banco da aplicação é criado à parte: --database-name do "create" só vale para cluster elástico.
az postgres flexible-server db show -g "$RESOURCE_GROUP" -s "$PG_SERVER_NAME" -d "$PG_DATABASE" --output none 2>/dev/null \
  || az postgres flexible-server db create -g "$RESOURCE_GROUP" -s "$PG_SERVER_NAME" -d "$PG_DATABASE" --output none
PG_FQDN="$(az postgres flexible-server show -g "$RESOURCE_GROUP" -n "$PG_SERVER_NAME" --query fullyQualifiedDomainName -o tsv)"
echo "    PostgreSQL: $PG_FQDN:5432/$PG_DATABASE"

echo "==> 3/6 Firewall: libera o seu IP para rodar os SELECTs (remova depois da entrega)"
CLIENT_IP="${CLIENT_IP:-$(curl -s https://api.ipify.org || true)}"
if [ -n "$CLIENT_IP" ]; then
  az postgres flexible-server firewall-rule create -g "$RESOURCE_GROUP" -n "$PG_SERVER_NAME" \
    --rule-name AllowClientIP --start-ip-address "$CLIENT_IP" --end-ip-address "$CLIENT_IP" --output none
  echo "    liberado: $CLIENT_IP"
else
  echo "    IP não detectado; defina CLIENT_IP no .env.azure e rode de novo."
fi

echo "==> 4/6 Usuário da aplicação (dono do schema do app; não é o administrador)"
export PGSSLMODE=require
pg_admin() { # pg_admin <database> <sql> [opções do psql]
  local db="$1" sql="$2"; shift 2
  PGPASSWORD="$PG_ADMIN_PASSWORD" psql -h "$PG_FQDN" -U "$PG_ADMIN_USER" -d "$db" -v ON_ERROR_STOP=1 -q "$@" -c "$sql"
}
# a regra de firewall do passo 3 pode levar alguns segundos para valer
for i in $(seq 1 12); do
  pg_admin postgres "SELECT 1" -tA >/dev/null 2>&1 && break
  [ "$i" = 12 ] && { echo "ERRO: não consegui conectar ao PostgreSQL com o administrador (firewall, IP ou senha)."; exit 1; }
  echo "    aguardando o firewall liberar o seu IP ($i/12)..."; sleep 10
done
if [ "$(pg_admin postgres "SELECT 1 FROM pg_roles WHERE rolname = '$DB_USERNAME'" -tA)" = "1" ]; then
  pg_admin postgres "ALTER ROLE \"$DB_USERNAME\" LOGIN PASSWORD '$DB_PASSWORD'"
else
  pg_admin postgres "CREATE ROLE \"$DB_USERNAME\" LOGIN PASSWORD '$DB_PASSWORD'"
fi
pg_admin "$PG_DATABASE" "GRANT ALL ON DATABASE \"$PG_DATABASE\" TO \"$DB_USERNAME\""
pg_admin "$PG_DATABASE" "GRANT ALL ON SCHEMA public TO \"$DB_USERNAME\""
echo "    usuário $DB_USERNAME pronto"

echo "==> 5/6 App Service Plan $APP_SERVICE_PLAN (Linux B1)"
az appservice plan show -g "$RESOURCE_GROUP" -n "$APP_SERVICE_PLAN" --output none 2>/dev/null \
  || az appservice plan create -g "$RESOURCE_GROUP" -n "$APP_SERVICE_PLAN" -l "$LOCATION" --is-linux --sku B1 --output none

echo "==> 6/6 Web App $WEB_APP_NAME (Java 21) e variáveis de ambiente"
az webapp show -g "$RESOURCE_GROUP" -n "$WEB_APP_NAME" --output none 2>/dev/null \
  || az webapp create -g "$RESOURCE_GROUP" -p "$APP_SERVICE_PLAN" -n "$WEB_APP_NAME" \
       --runtime "JAVA:21-java21" --https-only true --output none
az webapp config set -g "$RESOURCE_GROUP" -n "$WEB_APP_NAME" --always-on true --output none
az webapp log config -g "$RESOURCE_GROUP" -n "$WEB_APP_NAME" --docker-container-logging filesystem --output none

# Mesmos App Settings que o CD grava a cada deploy (aqui só para o primeiro start).
export DB_URL="jdbc:postgresql://${PG_FQDN}:5432/${PG_DATABASE}?sslmode=require"
SETTINGS_FILE="$(mktemp)"; trap 'rm -f "$SETTINGS_FILE"' EXIT; chmod 600 "$SETTINGS_FILE"
URL="https://$(web_host)" python3 - "$SETTINGS_FILE" <<'PY'
import json, os, sys
e = os.environ
json.dump({"DB_URL": e["DB_URL"], "DB_USERNAME": e["DB_USERNAME"], "DB_PASSWORD": e["DB_PASSWORD"],
           "JWT_SECRET": e["JWT_SECRET"], "CORS_ALLOWED_ORIGINS": e["URL"], "WEBSITES_PORT": "8080",
           "WEBSITES_CONTAINER_START_TIME_LIMIT": "600"}, open(sys.argv[1], "w"))
PY
az webapp config appsettings set -g "$RESOURCE_GROUP" -n "$WEB_APP_NAME" --settings "@$SETTINGS_FILE" --output none

echo
echo "Infraestrutura pronta."
echo "  Web App    : https://$(web_host)   (Swagger: /swagger)"
echo "  PostgreSQL : $PG_FQDN:5432/$PG_DATABASE"
echo
echo "Cadastre no Variable Group 'petflow-s4-secrets' (Pipelines > Library):"
echo "  DB_URL = $DB_URL"
echo "  DB_USERNAME, DB_PASSWORD, JWT_SECRET (do .env.azure) | SMOKE_TUTOR_EMAIL, SMOKE_TUTOR_PASSWORD"
az resource list -g "$RESOURCE_GROUP" --query "[].{recurso:name, tipo:type}" --output table
