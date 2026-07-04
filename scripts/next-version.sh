#!/bin/sh
# Calcula a próxima versão (Semantic Versioning) com base nos commits desde a última tag.
# - fix/bug/correção → PATCH (1.0.17 → 1.0.18)
# - feat/melhoria/improvement/perf → MINOR (1.0.17 → 1.1.0)
# Uso: ./scripts/next-version.sh [BUILD_NUMBER para fallback quando não há tags]

set -e

BUILD_NUMBER="${1:-0}"

# Última tag (ex: v1.0.17); se não houver, usa 1.0.BUILD_NUMBER
LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || true)
if [ -z "$LATEST_TAG" ]; then
  echo "1.0.${BUILD_NUMBER}"
  exit 0
fi

# Remove 'v' e extrai MAJOR.MINOR.PATCH
VERSION="${LATEST_TAG#v}"
MAJOR=$(echo "$VERSION" | cut -d. -f1)
MINOR=$(echo "$VERSION" | cut -d. -f2)
PATCH=$(echo "$VERSION" | cut -d. -f3)

# Commits desde a última tag: algum indica melhoria/feat?
COMMITS_SINCE="${LATEST_TAG}..HEAD"
COMMIT_MSGS=$(git log "$COMMITS_SINCE" --pretty=format:"%s" 2>/dev/null || true)
BUMP_MINOR=0
if [ -n "$COMMIT_MSGS" ]; then
  if echo "$COMMIT_MSGS" | tr '[:upper:]' '[:lower:]' | grep -qE '(feat|melhoria|improvement|perf|feature)[!:\s]'; then
    BUMP_MINOR=1
  fi
fi

if [ "$BUMP_MINOR" -eq 1 ]; then
  MINOR=$((MINOR + 1))
  PATCH=0
  echo "${MAJOR}.${MINOR}.${PATCH}"
else
  PATCH=$((PATCH + 1))
  echo "${MAJOR}.${MINOR}.${PATCH}"
fi
