#!/bin/sh
# Payanam Rail - ready-to-run build. Needs only Java 17 or newer.
cd "$(dirname "$0")"
echo "Starting Payanam Rail on http://localhost:8080 ..."
( sleep 15; (command -v open >/dev/null && open http://localhost:8080) || (command -v xdg-open >/dev/null && xdg-open http://localhost:8080) ) >/dev/null 2>&1 &
exec java -jar payanam-rail-1.0.0.jar
