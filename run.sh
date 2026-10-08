#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$script_dir"

if ! command -v javac >/dev/null 2>&1; then
  echo "Java JDK 17 or newer is required. Install Java and try again."
  exit 1
fi

mkdir -p bin
source_list="${TMPDIR:-/tmp}/trainsim-sources-$$.txt"
trap 'rm -f "$source_list"' EXIT
find src -type f -name '*.java' -print > "$source_list"
javac -d bin @"$source_list"
mkdir -p bin/tsim/ui
cp -R src/TSim/ui/bitmaps bin/tsim/ui/
java -cp bin JavaMain
