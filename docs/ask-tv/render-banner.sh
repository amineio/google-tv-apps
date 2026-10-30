#!/usr/bin/env bash
# Render the existing chat composition with the current AI Chat display name.
# Requires magick, cwebp, and Python 3 with FontTools.
# Reproduce: uv run --no-project --with fonttools==4.68.0 bash docs/ask-tv/render-banner.sh
set -euo pipefail
ASK_REPO=$(cd "$(dirname "$0")/../.." && pwd)
ASK_TEMP=$(mktemp -d)
trap 'rm -rf "$ASK_TEMP"' EXIT
# FontTools is a render-time dependency. The static font stays in this temporary directory.
python3 - "$ASK_REPO/apps/ask-tv/src/main/res/font/outfit.ttf" "$ASK_TEMP/outfit-semibold.ttf" <<'PYFONT'
import sys
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

font = TTFont(sys.argv[1])
instantiateVariableFont(font, {"wght": 600}, inplace=True)
font.save(sys.argv[2])
PYFONT
magick -size 640x360 xc:'#0D0E12' -fill '#0D0E12' -stroke '#F6CD83' -strokewidth 6 \
  -draw 'roundrectangle 123,157 225,234 14,14' -draw 'path "M 225,211 L 225,249 L 199,232"' \
  -draw 'roundrectangle 82,119 188,200 15,15' -draw 'path "M 83,176 L 83,215 L 111,199"' \
  -stroke none -fill '#FFFFFF' -font "$ASK_TEMP/outfit-semibold.ttf" \
  -pointsize 72 -annotate +258+207 'AI Chat' "$ASK_TEMP/banner.png"
cwebp -quiet -q 90 "$ASK_TEMP/banner.png" -o "$ASK_REPO/apps/ask-tv/src/main/res/drawable-nodpi/ask_banner.webp"
