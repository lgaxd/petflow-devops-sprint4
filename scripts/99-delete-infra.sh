#!/usr/bin/env bash
# Apaga TODA a infraestrutura do Sprint 4 (Resource Group). Não toca na infra de Java.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
require_vars RESOURCE_GROUP
require_az
read -r -p "Digite o nome do Resource Group ($RESOURCE_GROUP) para apagar: " ANS
[ "$ANS" = "$RESOURCE_GROUP" ] || { echo "Cancelado."; exit 1; }
az group delete --name "$RESOURCE_GROUP" --yes --no-wait
echo "Exclusão iniciada."
