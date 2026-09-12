#!/usr/bin/env bash
# Erzeugt ios/Logbot.xcodeproj aus ios/project.yml.
#
# Das Projekt liegt bewusst nicht im Repository: Eine project.pbxproj wird von
# Xcode bei jeder Aenderung neu durchgewuerfelt und erzeugt Konflikte, die
# niemand sinnvoll aufloesen kann. XcodeGen baut sie aus einer lesbaren
# YAML-Datei - vor jedem Build, in der CI wie auf dem eigenen Rechner.
#
# Aufruf: bash scripts/ensure_ios_project.sh
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root/ios"

if ! command -v xcodegen >/dev/null 2>&1; then
  if command -v brew >/dev/null 2>&1; then
    echo "XcodeGen fehlt - wird per Homebrew nachgeholt."
    brew install xcodegen
  else
    echo "::error::XcodeGen fehlt und Homebrew ist nicht da. Installation: brew install xcodegen"
    exit 1
  fi
fi

xcodegen generate --spec project.yml
echo "ios/Logbot.xcodeproj ist erzeugt."
