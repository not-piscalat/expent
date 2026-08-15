#!/usr/bin/env bash
#
# Expent release workflow — one command: bump version, test, build the signed
# APK, commit, tag, and publish a GitHub release with the APK attached.
#
# Usage:
#   ./scripts/release.sh 0.2.0
#
# Optional: override the release notes with RELEASE_NOTES="..." (otherwise a
# short default is used).
#
# Requirements:
#   - GitHub CLI installed and authenticated (`gh auth status`)
#   - keystore.properties + app/keystore/ present (release signing)
#   - JAVA_HOME set, or the default JDK path below valid on this machine
#
# This script commits the version bump, pushes main, tags the release, pushes
# the tag, and creates the GitHub release. The keystore is never touched.

set -euo pipefail

VERSION="${1:?Usage: ./scripts/release.sh <version>   e.g. ./scripts/release.sh 0.2.0}"

# --- Resolve tools -----------------------------------------------------------
export JAVA_HOME="${JAVA_HOME:-C:/Program Files/Java/jdk-22}"
GH="$(command -v gh 2>/dev/null || echo "/c/Program Files/GitHub CLI/gh.exe")"

# --- Validate ----------------------------------------------------------------
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || {
    echo "error: version must look like X.Y.Z (got '$VERSION')" >&2
    exit 1
}
[[ -f keystore.properties ]] || {
    echo "error: keystore.properties not found — release signing not configured" >&2
    exit 1
}
"$GH" auth status &>/dev/null || {
    echo "error: GitHub CLI is not authenticated — run 'gh auth login'" >&2
    exit 1
}

BUILD_FILE="app/build.gradle.kts"
git diff --quiet "$BUILD_FILE" || {
    echo "error: $BUILD_FILE has uncommitted changes — commit or stash them first" >&2
    exit 1
}

# --- Bump version ------------------------------------------------------------
CURRENT_CODE="$(grep -oP 'versionCode = \K[0-9]+' "$BUILD_FILE")"
NEW_CODE=$((CURRENT_CODE + 1))
sed -i \
    -e "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" \
    -e "s/versionName = \"[0-9][0-9.]*\"/versionName = \"$VERSION\"/" \
    "$BUILD_FILE"
echo "→ Bumped to v$VERSION (versionCode $CURRENT_CODE -> $NEW_CODE)"

# --- Test + build ------------------------------------------------------------
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleRelease
APK="app/build/outputs/apk/release/app-release.apk"
[[ -f "$APK" ]] || { echo "error: no APK produced at $APK" >&2; exit 1; }

# --- Commit + push -----------------------------------------------------------
git add "$BUILD_FILE"
git commit -m "Bump version to $VERSION"
git push origin main

# --- Tag + release -----------------------------------------------------------
git tag -a "v$VERSION" -m "Expent v$VERSION"
git push origin "v$VERSION"

NOTES="${RELEASE_NOTES:-Expent v$VERSION — see the README for features and install instructions.}"
"$GH" release create "v$VERSION" \
    --title "Expent v$VERSION" \
    --notes "$NOTES" \
    "$APK"

echo "✅ Released Expent v$VERSION: https://github.com/not-piscalat/expent/releases/tag/v$VERSION"
