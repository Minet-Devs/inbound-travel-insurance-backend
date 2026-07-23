#!/bin/bash
# Regenerates underwriter-workflow.pdf from underwriter-workflow.html
# Usage: ./generate-pdf.sh
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  --headless --disable-gpu \
  --print-to-pdf="$DIR/underwriter-workflow.pdf" \
  --no-pdf-header-footer \
  "$DIR/underwriter-workflow.html"

echo "Generated $DIR/underwriter-workflow.pdf"
