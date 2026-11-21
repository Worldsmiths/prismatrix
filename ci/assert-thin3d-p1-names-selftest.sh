#!/usr/bin/env bash
# Planted-name proof for ci/assert-thin3d-p1-names.sh (implement-spec Thin3D P1 V9).
# Plants a forbidden P1 name under game/src, expects the fence to fail, cleans up,
# then expects a clean pass. Leaves the tree clean even on failure (trap).
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
assert="$root/ci/assert-thin3d-p1-names.sh"
planted="$root/game/src/Thin3dP1NameFenceSelftestPlant.java"

cleanup() {
  rm -f "$planted"
}
trap cleanup EXIT

cleanup
cat >"$planted" <<'EOF'
// Temporary plant for Thin3D P1 name-fence selftest; must not remain in tree.
class Thin3dP1NameFenceSelftestPlant {
  void probe() {
    OrthographicCamera camera = null;
  }
}
EOF

if "$assert"; then
  echo "thin3d-p1-names-selftest FAIL: fence passed with planted OrthographicCamera" >&2
  exit 1
fi

cleanup

if ! "$assert"; then
  echo "thin3d-p1-names-selftest FAIL: fence failed after plant cleanup" >&2
  exit 1
fi

echo "thin3d-p1-names-selftest OK: planted name rejected; clean tree passes"
