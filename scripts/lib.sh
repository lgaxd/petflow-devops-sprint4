#!/usr/bin/env bash
# Funções comuns dos scripts de infraestrutura (rodar no WSL/Git Bash com Azure CLI logado).
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# tr -d '\r': o editor do Windows grava CRLF e o \r viraria parte do valor das variáveis
if [ -f "$ROOT/.env.azure" ]; then set -a; source <(tr -d '\r' < "$ROOT/.env.azure"); set +a; fi

require_vars() {
  local missing=0
  for v in "$@"; do
    if [ -z "${!v:-}" ]; then echo "ERRO: defina $v no .env.azure"; missing=1; fi
  done
  [ "$missing" = 0 ] || exit 1
}

require_az() {
  command -v az >/dev/null || { echo "ERRO: Azure CLI não encontrada (az)."; exit 1; }
  az account show --query "{assinatura:name, usuario:user.name}" --output table \
    || { echo "ERRO: rode 'az login' primeiro."; exit 1; }
}

web_host() { az webapp show --resource-group "$RESOURCE_GROUP" --name "$WEB_APP_NAME" --query defaultHostName --output tsv; }
