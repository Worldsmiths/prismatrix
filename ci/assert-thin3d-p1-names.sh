#!/usr/bin/env bash
# Temporary Thin3D P1 name fence (implement-spec Thin3D P1 AC9 / V9).
# Fails if game/src Java mentions P1 API names before the first intentional call PR.
# Removable with that PR (+ WORLDSMITHS_ENGINE_REF pin bump). Do not treat as permanent policy.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
game_src="$root/game/src"

# Settled P1 author surface names; createOrthographicCamera included as the ortho factory.
pattern='OrthographicCamera|getPickRay|createBoxEmissive|createSphereEmissive|setTranslationRotationScaling|createOrthographicCamera'

hits="$(grep -RIn --include='*.java' -E "$pattern" "$game_src" || true)"
if [[ -n "$hits" ]]; then
  echo "thin3d-p1-names FAIL: Thin3D P1 API name(s) in game sources (temporary fence; remove with first intentional P1 call PR + pin bump):" >&2
  echo "$hits" >&2
  exit 1
fi

echo "thin3d-p1-names OK: no Thin3D P1 API names in game/src"
