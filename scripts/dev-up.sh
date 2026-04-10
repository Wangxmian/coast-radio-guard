#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"
LOG_DIR="$ROOT_DIR/logs"

BACKEND_PID_FILE="$RUN_DIR/backend.pid"
AI_PID_FILE="$RUN_DIR/ai-service.pid"
FRONTEND_PID_FILE="$RUN_DIR/frontend.pid"

mkdir -p "$RUN_DIR" "$LOG_DIR"

is_running() {
  local pid_file="$1"
  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(cat "$pid_file")"
    if ps -p "$pid" >/dev/null 2>&1; then
      return 0
    fi
  fi
  return 1
}

start_docker_deps() {
  echo "[1/5] 启动 MySQL（Redis 复用本机 6379）..."
  cd "$ROOT_DIR/docker"
  docker compose -f docker-compose.dev.yml up -d mysql
}

init_db() {
  echo "[2/5] 初始化数据库 schema..."
  local retries=25
  local ok=0

  wait_mysql_ready() {
    for ((i=1; i<=retries; i++)); do
      if docker exec crg-mysql mysql -uroot -proot -e "SELECT 1" >/dev/null 2>&1; then
        ok=1
        return 0
      fi
      sleep 2
    done
    return 1
  }

  if ! wait_mysql_ready; then
    echo "MySQL 首次启动未就绪，尝试自动重建 MySQL 数据卷..."
    cd "$ROOT_DIR/docker"
    docker compose -f docker-compose.dev.yml down -v
    docker compose -f docker-compose.dev.yml up -d mysql
    ok=0
    if ! wait_mysql_ready; then
      echo "MySQL 自动修复后仍未就绪，请查看: docker logs crg-mysql"
      exit 1
    fi
  fi

  docker exec -i crg-mysql mysql --default-character-set=utf8mb4 -uroot -proot < "$ROOT_DIR/backend/sql/init.sql"
}

start_backend() {
  if is_running "$BACKEND_PID_FILE"; then
    echo "[3/5] backend 已在运行，跳过"
    return
  fi

  echo "[3/5] 启动 backend..."
  cd "$ROOT_DIR/backend"
  nohup ./mvnw spring-boot:run > "$LOG_DIR/backend.log" 2>&1 &
  echo $! > "$BACKEND_PID_FILE"
}

start_ai_service() {
  if is_running "$AI_PID_FILE"; then
    echo "[4/5] ai-service 已在运行，跳过"
    return
  fi

  echo "[4/5] 启动 ai-service..."
  cd "$ROOT_DIR/ai-service"
  if [[ ! -f ".env" && -f ".env.example" ]]; then
    cp .env.example .env
  fi
  if [[ ! -d ".venv" ]]; then
    python3 -m venv .venv
  fi
  # shellcheck disable=SC1091
  source .venv/bin/activate
  pip install -r requirements.txt >/dev/null
  local use_real_vad="${AI_USE_REAL_VAD:-}"
  local use_real_asr="${AI_USE_REAL_ASR:-}"
  local use_real_se="${AI_USE_REAL_SE:-}"
  local use_real_llm="${AI_USE_REAL_LLM:-}"
  local se_provider="${SE_PROVIDER:-}"
  read_env_value() {
    local key="$1"
    local file="$2"
    if [[ ! -f "$file" ]]; then
      return 0
    fi
    local line
    line="$(grep -E "^${key}=" "$file" | tail -n 1 || true)"
    if [[ -n "$line" ]]; then
      echo "${line#*=}"
    fi
  }
  if [[ -f ".env" ]]; then
    # 仅读取需要的开关，不 source .env，避免空值覆盖外部环境变量（如 API key）
    local env_real_vad env_real_asr env_real_se env_real_llm env_se_provider
    env_real_vad="$(read_env_value "AI_USE_REAL_VAD" ".env")"
    env_real_asr="$(read_env_value "AI_USE_REAL_ASR" ".env")"
    env_real_se="$(read_env_value "AI_USE_REAL_SE" ".env")"
    env_real_llm="$(read_env_value "AI_USE_REAL_LLM" ".env")"
    env_se_provider="$(read_env_value "SE_PROVIDER" ".env")"
    # 优先级：shell/process env > .env > default
    # 若外部环境变量已设置非空值，不允许被 .env 覆盖。
    if [[ -z "${use_real_vad:-}" && -n "${env_real_vad:-}" ]]; then
      use_real_vad="$env_real_vad"
    fi
    if [[ -z "${use_real_asr:-}" && -n "${env_real_asr:-}" ]]; then
      use_real_asr="$env_real_asr"
    fi
    if [[ -z "${use_real_se:-}" && -n "${env_real_se:-}" ]]; then
      use_real_se="$env_real_se"
    fi
    if [[ -z "${use_real_llm:-}" && -n "${env_real_llm:-}" ]]; then
      use_real_llm="$env_real_llm"
    fi
    if [[ -z "${se_provider:-}" && -n "${env_se_provider:-}" ]]; then
      se_provider="$env_se_provider"
    fi
  fi
  local use_real_vad_lc use_real_asr_lc use_real_se_lc use_real_llm_lc se_provider_lc
  use_real_vad_lc="$(echo "${use_real_vad:-}" | tr '[:upper:]' '[:lower:]')"
  use_real_asr_lc="$(echo "${use_real_asr:-}" | tr '[:upper:]' '[:lower:]')"
  use_real_se_lc="$(echo "${use_real_se:-}" | tr '[:upper:]' '[:lower:]')"
  use_real_llm_lc="$(echo "${use_real_llm:-}" | tr '[:upper:]' '[:lower:]')"
  se_provider_lc="$(echo "${se_provider:-}" | tr '[:upper:]' '[:lower:]')"
  if [[ "$use_real_vad_lc" == "true" || "$use_real_asr_lc" == "true" || "$use_real_llm_lc" == "true" ]]; then
    if [[ -f "requirements-real.txt" ]]; then
      pip install -r requirements-real.txt >/dev/null
    fi
  fi
  if [[ "$use_real_se_lc" == "true" && "$se_provider_lc" == "gtcrn" ]]; then
    if [[ -f "requirements-gtcrn.txt" ]]; then
      pip install -r requirements-gtcrn.txt >/dev/null
    fi
  fi
  # 一键启动优先稳定性，避免 watch .venv 导致频繁重载和前端 Network Error
  nohup uvicorn app.main:app --host 0.0.0.0 --port 8000 > "$LOG_DIR/ai-service.log" 2>&1 &
  echo $! > "$AI_PID_FILE"
}

start_frontend() {
  if is_running "$FRONTEND_PID_FILE"; then
    echo "[5/5] frontend 已在运行，跳过"
    return
  fi

  echo "[5/5] 启动 frontend..."
  cd "$ROOT_DIR/frontend"
  npm install >/dev/null
  nohup npm run dev -- --host 0.0.0.0 --port 5173 > "$LOG_DIR/frontend.log" 2>&1 &
  echo $! > "$FRONTEND_PID_FILE"
}

print_result() {
  echo
  echo "全部启动命令已执行。"
  echo "frontend: http://127.0.0.1:5173"
  echo "backend : http://127.0.0.1:18080/api/health"
  echo "ai      : http://127.0.0.1:8000/api/health"
  echo
  echo "日志目录: $LOG_DIR"
  echo "状态查看: ./scripts/dev-status.sh"
  echo "停止服务: ./scripts/dev-down.sh"
}

start_docker_deps
init_db
start_backend
start_ai_service
start_frontend
print_result
