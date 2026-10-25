#!/usr/bin/env bash
set -euo pipefail

: "${GPG_SECRET_KEY:?GPG_SECRET_KEY is required}"
: "${GPG_PASSWORD:?GPG_PASSWORD is required}"
: "${GPG_KEY_ID:?GPG_KEY_ID is required}"

export GNUPGHOME
GNUPGHOME="$(mktemp -d)"
chmod 700 "$GNUPGHOME"
trap 'rm -rf "$GNUPGHOME"' EXIT

printf '%s\n' "$GPG_SECRET_KEY" | gpg --batch --import

shopt -s nullglob
files=(dist/*.jar dist/*.pom dist/*.zip)
test "${#files[@]}" -gt 0

for file in "${files[@]}"; do
  printf '%s\n' "$GPG_PASSWORD" |
    gpg --batch --yes --pinentry-mode loopback --passphrase-fd 0 \
      --local-user "$GPG_KEY_ID" --armor --detach-sign \
      --output "$file.asc" "$file"
  gpg --batch --verify "$file.asc" "$file"
done

(
  cd dist
  sha256sum *.jar *.pom *.zip *.asc > SHA256SUMS
  sha256sum -c SHA256SUMS
)

printf '%s\n' "$GPG_PASSWORD" |
  gpg --batch --yes --pinentry-mode loopback --passphrase-fd 0 \
    --local-user "$GPG_KEY_ID" --armor --detach-sign \
    --output dist/SHA256SUMS.asc dist/SHA256SUMS
gpg --batch --verify dist/SHA256SUMS.asc dist/SHA256SUMS
