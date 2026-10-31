#!/usr/bin/env sh
# Installs ATR (Android Test Runner) for the current user.
# Copies this distribution to ~/.local/share/atr and links ~/.local/bin/atr.
#   ./install.sh                 install (ATR_HOME=/custom/dir ./install.sh to change the folder)
#   ./install.sh --uninstall     remove
set -eu

DIR="${ATR_HOME:-$HOME/.local/share/atr}"
LINK_DIR="$HOME/.local/bin"
SRC="$(cd "$(dirname "$0")" && pwd)"

if [ "${1:-}" = "--uninstall" ]; then
  rm -rf "$DIR" "$LINK_DIR/atr"
  echo "ATR removed from $DIR"
  exit 0
fi

[ -f "$SRC/bin/atr" ] || { echo "Run this script from the unzipped ATR folder (bin/atr not found)." >&2; exit 1; }

command -v java >/dev/null 2>&1 || [ -n "${JAVA_HOME:-}" ] || echo "WARNING: Java 17+ not found (java on PATH or JAVA_HOME)."
command -v adb >/dev/null 2>&1 || [ -n "${ANDROID_HOME:-}" ] || echo "WARNING: adb not found (PATH or ANDROID_HOME)."
command -v scrcpy >/dev/null 2>&1 || echo "WARNING: scrcpy not found: ATR will run without videos."

if [ "$SRC" != "$DIR" ]; then
  rm -rf "$DIR"
  mkdir -p "$DIR"
  cp -R "$SRC/." "$DIR/"
fi
chmod +x "$DIR/bin/atr"
mkdir -p "$LINK_DIR"
ln -sf "$DIR/bin/atr" "$LINK_DIR/atr"

echo "ATR installed to $DIR"
case ":$PATH:" in
  *":$LINK_DIR:"*) echo "Run: atr version" ;;
  *) echo "Add $LINK_DIR to PATH (e.g. in ~/.profile), then run: atr version" ;;
esac
