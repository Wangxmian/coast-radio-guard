#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"

print_status() {
  local name="$1"
  local pid_file="$2"
  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(cat "$pid_file")"
    if ps -p "$pid" >/dev/null 2>&1; then
      echo "$name: RUNNING (pid=$pid)"
      return
    fi
  fi
  echo "$name: STOPPED"
}

print_status "backend" "$RUN_DIR/backend.pid"
print_status "ai-service" "$RUN_DIR/ai-service.pid"
print_status "frontend" "$RUN_DIR/frontend.pid"

echo "docker containers:"
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | sed -n '1,20p'
