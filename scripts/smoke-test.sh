#!/usr/bin/env bash
# Smoke test do app já implantado: login + CRUD completo de Pets pela URL pública.
# Falha (exit 1) se qualquer passo não devolver o status esperado.
# Variáveis: RESOURCE_GROUP, WEB_APP_NAME, SMOKE_TUTOR_EMAIL, SMOKE_TUTOR_PASSWORD
# (ou BASE_URL no lugar das duas primeiras).
set -euo pipefail

if [ -f "$(dirname "$0")/../.env.azure" ]; then set -a; source <(tr -d '\r' < "$(dirname "$0")/../.env.azure"); set +a; fi
: "${SMOKE_TUTOR_EMAIL:?defina SMOKE_TUTOR_EMAIL}" "${SMOKE_TUTOR_PASSWORD:?defina SMOKE_TUTOR_PASSWORD}"
if [ -z "${BASE_URL:-}" ]; then
  : "${RESOURCE_GROUP:?}" "${WEB_APP_NAME:?}"
  BASE_URL="https://$(az webapp show -g "$RESOURCE_GROUP" -n "$WEB_APP_NAME" --query defaultHostName -o tsv)"
fi

TMP="$(mktemp)"; trap 'rm -f "$TMP"' EXIT
FAIL=0

# call METHOD PATH EXPECTED [TOKEN] [JSON_BODY] -> grava o corpo em $TMP
call() {
  local method="$1" path="$2" expect="$3" token="${4:-}" body="${5:-}"
  local args=(-s -o "$TMP" -w "%{http_code}" --max-time 30 -X "$method" "$BASE_URL$path")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ] && args+=(-H "Content-Type: application/json" -d "$body")
  local code; code="$(curl "${args[@]}" || true)"
  if [ "$code" = "$expect" ]; then echo "PASS $method $path -> $code"
  else echo "FAIL $method $path -> $code (esperado $expect): $(head -c 300 "$TMP")"; FAIL=1; fi
}
json_field() { python3 -c "import json,sys; print(json.load(open('$TMP'))['$1'])"; }

echo "==> Smoke test em $BASE_URL"
call GET /actuator/health 200
call GET /pets 401                                   # sem token não entra
call POST /auth/login 200 "" "{\"email\":\"$SMOKE_TUTOR_EMAIL\",\"password\":\"$SMOKE_TUTOR_PASSWORD\"}"
[ "$FAIL" = 0 ] || { echo "Login falhou; abortando."; exit 1; }
TOKEN="$(json_field token)"; TUTOR_ID="$(json_field id)"

PET='{"name":"Smoke Test","breed":"SRD","birthDate":"2020-01-01","weight":10.5,"tutorId":'$TUTOR_ID',"speciesId":1}'
call POST /pets 201 "$TOKEN" "$PET"
[ "$FAIL" = 0 ] || exit 1
PET_ID="$(json_field id)"
call GET "/pets/$PET_ID" 200 "$TOKEN"
call PUT "/pets/$PET_ID" 200 "$TOKEN" "${PET/Smoke Test/Smoke Test Atualizado}"
call GET "/pets/$PET_ID" 200 "$TOKEN"
grep -q "Smoke Test Atualizado" "$TMP" || { echo "FAIL: PUT não persistiu o novo nome"; FAIL=1; }

# Agendamento: AGENDADO aceita data futura; REALIZADO com data futura é rejeitado (422)
FUTURE="$(date -d '+30 days' +%F)"
EVENT='{"description":"Smoke retorno","eventDate":"'$FUTURE'","status":"AGENDADO","petId":'$PET_ID',"eventTypeId":1}'
call POST /health-events 201 "$TOKEN" "$EVENT"
EVENT_ID="$(json_field id)"
call POST /health-events 422 "$TOKEN" "${EVENT/AGENDADO/REALIZADO}"
grep -q FUTURE_DATE_NOT_ALLOWED "$TMP" || { echo "FAIL: código FUTURE_DATE_NOT_ALLOWED ausente"; FAIL=1; }
call DELETE "/health-events/$EVENT_ID" 204 "$TOKEN"

call DELETE "/pets/$PET_ID" 204 "$TOKEN"
call GET "/pets/$PET_ID" 404 "$TOKEN"                # prova de que foi removido

if [ "$FAIL" = 0 ]; then echo "Smoke test OK: CRUD completo no app implantado."; else echo "Smoke test FALHOU."; exit 1; fi
