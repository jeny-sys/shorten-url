#!/usr/bin/env bash
# wrk 压测脚本
# 前置: 应用已起在 8080, MySQL/Redis 已就绪

set -euo pipefail

DURATION=${DURATION:-30s}
THREADS=${THREADS:-12}
CONNECTIONS=${CONNECTIONS:-400}

echo "=== 1. 预生成 100 个短链以制造热点 ==="
for i in $(seq 1 100); do
  curl -s -X POST http://localhost:8080/api/shorten \
    -H "Content-Type: application/json" \
    -d "{\"url\":\"https://example.com/seed/$i\"}" > /dev/null
done

# 2. 拿一个短码当压测目标
SAMPLE=$(curl -s -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com/benchmark"}' | \
  grep -oE '"shortCode":"[^"]+"' | cut -d'"' -f4)

echo "=== 2. 压测短链: http://localhost:8080/$SAMPLE ==="
echo "  threads=$THREADS  connections=$CONNECTIONS  duration=$DURATION"

# 3. 用 wrk 压测
WRK_CMD="wrk -t${THREADS} -c${CONNECTIONS} -d${DURATION} --latency http://localhost:8080/$SAMPLE"
echo "=== 3. 跳转接口压测 ==="
if command -v wrk >/dev/null 2>&1; then
  $WRK_CMD | tee wrk-result-redirect.txt
else
  # Windows 友好:用 Docker 跑 wrk
  docker run --rm --network=host williamyeh/wrk \
    -t${THREADS} -c${CONNECTIONS} -d${DURATION} --latency \
    "http://localhost:8080/$SAMPLE" | tee wrk-result-redirect.txt
fi

echo
echo "完成。结果在 wrk-result-redirect.txt"
