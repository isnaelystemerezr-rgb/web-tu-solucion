#!/usr/bin/env bash
# ============================================================================
# Arranca backend (Spring Boot, :8080) y frontend (Vite, :5173) juntos.
# Uso:  ./start.sh        (Ctrl+C detiene ambos)
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "▶ Backend en http://localhost:8080 ..."
(cd "$ROOT/backend" && mvn -q spring-boot:run) &
BACK_PID=$!

echo "▶ Frontend en http://localhost:5173 ..."
(cd "$ROOT/frontend" && npm install --silent && npm run dev) &
FRONT_PID=$!

trap 'echo; echo "■ Deteniendo..."; kill $BACK_PID $FRONT_PID 2>/dev/null || true; wait 2>/dev/null || true' INT TERM

wait
