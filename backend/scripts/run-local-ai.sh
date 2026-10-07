#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi

export OLLAMA_MODEL="${OLLAMA_MODEL:-gemma3:4b}"
redis_port="${TRIBE_LOCAL_REDIS_PORT:-6381}"
api_port="${TRIBE_LOCAL_API_PORT:-8083}"
app_url="${APP_URL:-http://localhost:8082}"

if ! curl --fail --silent --max-time 3 http://127.0.0.1:11434/api/tags >/dev/null; then
  printf 'Start Ollama first: OLLAMA_CONTEXT_LENGTH=8192 OLLAMA_NUM_PARALLEL=1 ollama serve\n' >&2
  exit 1
fi
if ! OLLAMA_HOST=127.0.0.1:11434 ollama show "$OLLAMA_MODEL" >/dev/null 2>&1; then
  printf 'Download the model first: OLLAMA_HOST=127.0.0.1:11434 ollama pull %s\n' "$OLLAMA_MODEL" >&2
  exit 1
fi
if [[ "$(redis-cli -h 127.0.0.1 -p "$redis_port" ping 2>/dev/null || true)" != PONG ]]; then
  printf "Start Redis first: redis-server --bind 127.0.0.1 --port %s --save '' --appendonly no\n" "$redis_port" >&2
  exit 1
fi

place_search=false
if [[ -n "${GOOGLE_KEY:-}" && "${GOOGLE_KEY}" != test-google-key ]]; then
  place_search=true
fi

printf 'Local AI API: http://localhost:%s | Frontend: %s | Model: %s\n' "$api_port" "$app_url" "$OLLAMA_MODEL"
printf 'Test account: seed.owner@tribe.local / password (temporary in-memory database)\n'
printf 'Google Places enabled: %s. Without a key, enable text-only items when applying a proposal.\n' "$place_search"

exec ./gradlew :api:bootRun --args="--spring.profiles.active=local --server.address=127.0.0.1 --server.port=$api_port --spring.datasource.url=jdbc:h2:mem:tribe-local-ai;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE --spring.datasource.username=sa --spring.datasource.password= --spring.datasource.driver-class-name=org.h2.Driver --spring.data.redis.host=127.0.0.1 --spring.data.redis.port=$redis_port --spring.data.redis.database=0 --tribe.seed.enabled=true --app.url=$app_url --trip.review.ai.provider=ollama --ollama.api.url=http://127.0.0.1:11434/api/generate --ollama.model=$OLLAMA_MODEL --tribe.itinerary.place-search.enabled=$place_search --spring.jpa.properties.hibernate.show_sql=false"
