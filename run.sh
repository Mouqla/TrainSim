#!/usr/bin/env sh
set -eu

if ! command -v javac >/dev/null 2>&1; then
  echo "Java JDK 17 or newer is required. Install Java and try again."
  exit 1
fi

mkdir -p bin
javac -sourcepath src -d bin src/Main.java
java -cp bin JavaMain
