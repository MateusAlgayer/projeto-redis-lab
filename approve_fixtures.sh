#!/bin/sh

for r in $(find src/test/resources/characterization -name '*.received.md' | sort); do
  a="${r%.received.md}.approved.md"
  git diff --no-index "$a" "$r"
  printf 'Aprovar %s? [s/N/q] ' "${r##*/}"
  read -r resp
  case "$resp" in
    s) mv "$r" "$a" ;;
    q) break ;;
  esac
done