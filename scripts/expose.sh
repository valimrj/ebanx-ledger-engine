#!/usr/bin/env bash
# Sobe a aplicação, abre um túnel ngrok em :8080 e imprime a URL pública
# pronta pra colar em https://ipkiss.ebanx.ninja/. Ctrl+C encerra os dois.
# Requer o ngrok instalado e autenticado (ver README, seção "Como testar
# com o ipkiss da EBANX").
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

echo
echo "URL pública (cole em https://ipkiss.ebanx.ninja/):"
echo "  $URL"
echo
echo "Ctrl+C encerra a aplicação e o túnel."

wait "$APP_PID" "$NGROK_PID"
