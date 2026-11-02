#!/usr/bin/env bash
# Roda SQL no PostgreSQL da Azure (precisa do seu IP liberado: o script 01 cria a regra AllowClientIP).
# Requer o psql: sudo apt update && sudo apt install -y postgresql-client
# Uso:  bash scripts/sql.sh "SELECT id, name FROM pet ORDER BY id"
#       bash scripts/sql.sh -f database/crud-verification.sql
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
require_vars RESOURCE_GROUP PG_SERVER_NAME PG_DATABASE DB_USERNAME DB_PASSWORD
command -v psql >/dev/null || { echo "ERRO: psql não encontrado. Instale: sudo apt update && sudo apt install -y postgresql-client"; exit 1; }

HOST="$(az postgres flexible-server show -g "$RESOURCE_GROUP" -n "$PG_SERVER_NAME" --query fullyQualifiedDomainName -o tsv)"
export PGPASSWORD="$DB_PASSWORD" PGSSLMODE=require
if [ "${1:-}" = "-f" ]; then
  psql -h "$HOST" -U "$DB_USERNAME" -d "$PG_DATABASE" -f "$2"
else
  psql -h "$HOST" -U "$DB_USERNAME" -d "$PG_DATABASE" -c "$1"
fi
