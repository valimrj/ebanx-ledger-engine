#!/usr/bin/env bash
# Sobe a aplicação, abre um túnel ngrok em :8080, roda contra a URL
# pública a mesma sequência de cenários que o ipkiss verifica (reset,
# consulta de saldo, deposit, withdraw, transfer, valor inválido) e só
# então imprime a URL pronta pra colar em https://ipkiss.ebanx.ninja/.
#
# O ipkiss em si não tem API documentada nem formulário sem CSRF, então
# colar a URL e clicar em "Run tests!" lá continua manual — o que esse
# script garante é que, antes disso, a aplicação já passou no mesmo
# roteiro pela internet real, não só localmente.
#
# Ctrl+C encerra a aplicação e o túnel. Requer o ngrok instalado e
# autenticado (ver README, seção "Como testar com o ipkiss da EBANX").
set -euo pipefail

APP_LOG=$(mktemp)
NGROK_LOG=$(mktemp)

cleanup() {
  echo
  echo "Encerrando..."
  kill "${APP_PID:-}" "${NGROK_PID:-}" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo "Subindo a aplicação (log em $APP_LOG)..."
./gradlew run >"$APP_LOG" 2>&1 &
APP_PID=$!

echo "Aguardando a aplicação responder em http://localhost:8080..."
until curl -s -o /dev/null "http://localhost:8080/balance?account_id=healthcheck"; do
  sleep 1
done

echo "Aplicação no ar. Abrindo túnel ngrok..."
ngrok http 8080 --log=stdout >"$NGROK_LOG" 2>&1 &
NGROK_PID=$!

echo "Aguardando o ngrok abrir o túnel..."
URL=""
for _ in $(seq 1 10); do
  sleep 1
  URL=$(curl -s http://127.0.0.1:4040/api/tunnels \
    | grep -o '"public_url":"https:[^"]*"' \
    | head -1 \
    | sed -E 's/"public_url":"(.*)"/\1/')
  [ -n "$URL" ] && break
done

if [ -z "$URL" ]; then
  echo "Não consegui pegar a URL do ngrok. Veja o log: $NGROK_LOG"
  exit 1
fi

echo "Túnel aberto. Rodando o roteiro de cenários contra $URL..."
echo

FAILED=0

check() {
  local desc="$1" expected="$2"
  shift 2
  local out status
  out=$(curl -s -w '\n%{http_code}' "$@")
  status=$(tail -n1 <<<"$out")
  if [ "$status" = "$expected" ]; then
    echo "  OK   $desc"
  else
    local body
    body=$(sed '$d' <<<"$out")
    echo "  FAIL $desc (esperava $expected, veio $status; corpo: $body)"
    FAILED=1
  fi
}

check "reset limpa o estado"                200 -X POST "$URL/reset"
check "saldo de conta inexistente"          404 "$URL/balance?account_id=smoke"
check "deposit cria a conta"                201 -X POST "$URL/event" -H 'Content-Type: application/json' -d '{"type":"deposit","destination":"smoke","amount":10}'
check "saldo depois do deposit"             200 "$URL/balance?account_id=smoke"
check "withdraw com saldo suficiente"       201 -X POST "$URL/event" -H 'Content-Type: application/json' -d '{"type":"withdraw","origin":"smoke","amount":4}'
check "withdraw maior que o saldo"          400 -X POST "$URL/event" -H 'Content-Type: application/json' -d '{"type":"withdraw","origin":"smoke","amount":999}'
check "transfer entre contas"               201 -X POST "$URL/event" -H 'Content-Type: application/json' -d '{"type":"transfer","origin":"smoke","destination":"smoke-2","amount":6}'
check "amount zero ou negativo é rejeitado" 400 -X POST "$URL/event" -H 'Content-Type: application/json' -d '{"type":"deposit","destination":"smoke","amount":-1}'

echo
if [ "$FAILED" = "1" ]; then
  echo "Algum cenário falhou pela internet — reveja antes de colar no ipkiss."
else
  echo "Todos os cenários passaram pela internet real, não só localmente."
fi

echo
echo "URL pública (cole em https://ipkiss.ebanx.ninja/ e clique em \"Run tests!\"):"
echo "  $URL"
echo
echo "Ctrl+C encerra a aplicação e o túnel."

wait "$APP_PID" "$NGROK_PID"
