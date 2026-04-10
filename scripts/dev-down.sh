#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"

stop_by_pid_file() {
  local name="$1"
  local pid_file="$2"

  if [[ ! -f "$pid_file" ]]; then
    echo "$name 未发现 PID 文件，跳过"
    return
  fi

  local pid
  pid="$(cat "$pid_file")"
  if ps -p "$pid" >/dev/null 2>&1; then
    kill "$pid" >/dev/null 2>&1 || true
    sleep 1
    if ps -p "$pid" >/dev/null 2>&1; then
      kill -9 "$pid" >/dev/null 2>&1 || true
    fi
    echo "$name 已停止"
  else
    echo "$name 进程不存在，清理 PID 文件"
  fi

  rm -f "$pid_file"
}

stop_by_pid_file "backend" "$RUN_DIR/backend.pid"
stop_by_pid_file "ai-service" "$RUN_DIR/ai-service.pid"
stop_by_pid_file "frontend" "$RUN_DIR/frontend.pid"

echo "停止项目 docker 容器..."
cd "$ROOT_DIR/docker"
docker compose -f docker-compose.dev.yml down

echo "全部服务已停止"
