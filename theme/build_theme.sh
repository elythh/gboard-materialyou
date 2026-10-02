#!/bin/sh
# Packs theme/src into an Rboard-style theme pack (pack.meta + inner theme zip).
# Usage: theme/build_theme.sh [output.zip]
set -eu
here=$(cd "$(dirname "$0")" && pwd)
out=${1:-"$here/../build/3D_Template_MaterialYou.zip"}
case "$out" in /*) ;; *) out="$PWD/$out" ;; esac
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
(cd "$here/src" && zip -q -X -r "$work/3dTemplate_hndrk_materialyou.zip" .)
printf 'name=3D_Template_MaterialYou\nauthor=hndrk\n' > "$work/pack.meta"
mkdir -p "$(dirname "$out")"
rm -f "$out"
(cd "$work" && zip -q -X "$out" pack.meta 3dTemplate_hndrk_materialyou.zip)
echo "$out"
