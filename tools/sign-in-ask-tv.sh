#!/usr/bin/env bash
# The browser opens locally. Android owns the entire sign-in session.
set -euo pipefail
: "${TV_SERIAL:?set TV_SERIAL to the connected TV or emulator shown by adb devices}"
URL='http://127.0.0.1:1455/start'
OWNED_FORWARD=0
cleanup() {
  if [ "$OWNED_FORWARD" = 1 ] && adb -s "$TV_SERIAL" forward --list | awk -v serial="$TV_SERIAL" '$1 == serial && $2 == "tcp:1455" && $3 == "tcp:1455" { found=1 } END { exit !found }'; then
    adb -s "$TV_SERIAL" forward --remove tcp:1455 >/dev/null
  fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
adb -s "$TV_SERIAL" get-state >/dev/null
if adb -s "$TV_SERIAL" forward --list | awk '$2 == "tcp:1455" { found=1 } END { exit !found }'; then
  echo 'Local port 1455 already has an ADB forward. End that task before signing in.' >&2
  exit 1
fi
python3 - <<'PY'
import socket
import sys
with socket.socket() as listener:
    try:
        listener.bind(('127.0.0.1', 1455))
    except OSError:
        sys.exit('Local port 1455 is in use. Close that listener before signing in.')
PY
adb -s "$TV_SERIAL" forward --no-rebind tcp:1455 tcp:1455 >/dev/null
OWNED_FORWARD=1
READY=0
# Preserve an explicit plan-consent attempt already started on the TV.
if curl --silent --fail --max-time 2 http://127.0.0.1:1455/status >/dev/null 2>&1; then
  READY=1
else
  START=$(adb -s "$TV_SERIAL" shell am start -W -n com.amine.asktv/.MainActivity --ez authenticate true 2>&1)
  if ! printf '%s\n' "$START" | grep -q '^Status: ok'; then
    echo 'AI Chat did not open. Install AI Chat first.' >&2
    exit 1
  fi
  for ((attempt=0; attempt<60; attempt++)); do
    if curl --silent --fail --max-time 2 http://127.0.0.1:1455/status >/dev/null 2>&1; then READY=1; break; fi
    sleep 0.2
  done
fi
if [ "$READY" != 1 ]; then echo 'The TV sign-in listener is unavailable. Check AI Chat and try again.' >&2; exit 1; fi
open "$URL"
printf '%s\n' 'Finish sign-in in your browser. Keep AI Chat open. Cancel on the TV or press Ctrl-C here to close the tunnel.'
for ((attempt=0; attempt<600; attempt++)); do
  sleep 1
  if ! curl --silent --fail --max-time 2 http://127.0.0.1:1455/status >/dev/null 2>&1; then
    printf '%s\n' 'The browser flow ended. Check AI Chat for verified sign-in status.'
    exit 0
  fi
done
printf '%s\n' 'The sign-in window ended. Check AI Chat and start again if needed.'
