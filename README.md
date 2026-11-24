# Prismatrix

Outside-consumer Worldsmiths game (includeBuild of unpublished engine). **Playable for Puzzle World 1** on desktop: load packaged W1 levels (`goals` + `mirrorBudget`), place `/` and `\` on the live board, Test Fire ports, and Verify against Trace. Product intent lives in [`GDD.md`](GDD.md); author patterns in [`STRATEGIC_PLAYBOOK.md`](STRATEGIC_PLAYBOOK.md). The in-engine canary remains `samples/relay` in Worldsmiths; do not treat this tree as that canary.

## Play Puzzle W1

```bash
export WORLDSMITHS_ENGINE_ROOT=/path/to/worldsmiths   # if not ../worldsmiths
./gradlew :desktop:run
```

Loads `levels/w1/01.json`. Tap the tray `/` or `\` then an empty cell to place; tap a filled player cell to return a mirror; tap a border port for sticky **Test Fire** on the merged board; press **Enter** or **V** when remaining mirrors (and orbs, if any) are 0 to **Verify**.

## Thin3D P1 (M3)

Pitched puzzle / PvP board views consume Thin3D **P1** (ortho + pick-ray + emissive + TRS). CI pins a Worldsmiths SHA that includes P1 (`WORLDSMITHS_ENGINE_REF` in `.github/workflows/ci.yml`). The temporary P1 name fence used before that pin is gone; game sources may call those APIs.

## Bar C

- No direct `com.badlogicgames.gdx` dependency in `:game`
- No `com.badlogic.gdx` imports in game sources
- Transitive LibGDX via `ws-core` is OK

Enforce locally with `ci/assert-bar-c.sh`; CI runs the same gate on every push/PR.

## CI shape

CI stays **Bar C** + `:desktop:compileJava` + `:game:test`. There is **no** headed `:desktop:run` in CI.

## Requirements

- JDK 21+ to run Gradle
- A checkout of [Worldsmiths/worldsmiths](https://github.com/Worldsmiths/worldsmiths) (engine)
- Game module compiles for **Java 8** (`options.release = 8`)

## Engine path

| Context | Path |
| --- | --- |
| Local default | Sibling `../worldsmiths` |
| Override | `WORLDSMITHS_ENGINE_ROOT` or `-Dworldsmiths.engineRoot=...` |
| CI | Dual-checkout at `engine/` + `game/`; pin SHA in `.github/workflows/ci.yml` (`WORLDSMITHS_ENGINE_REF`) |

Uses bare `includeBuild` against the engine checkout (no `dependencySubstitution`).

## Local build

```bash
export WORLDSMITHS_ENGINE_ROOT=/path/to/worldsmiths   # if not ../worldsmiths
./gradlew :desktop:compileJava :game:test
```

## Soft Trace latency bench (optional)

HLD soft NFR: Trace p95 &lt; 0.5 ms headless on 8×8 (mirrors/orbs; no Splitter). This is a **showcase target**, not a CI fail gate. The harness prints p50/p95/max and only fails on an extreme multi-second wall timeout.

```bash
./gradlew :game:test --tests io.github.worldsmiths.prismatrix.optics.TraceLatencyBenchTest
```

Tagged `@Tag("bench")` in `TraceLatencyBenchTest` for optional tag filtering; default `:game:test` stays non-flaky.

## Founder headed smoke checklist (manual — not CI)

Canonical Mode 2 smoke (goal pads, Verify, lock mark) lives in [`STRATEGIC_PLAYBOOK.md`](STRATEGIC_PLAYBOOK.md) **Part 6 — Founder smoke checklist (manual — V8)**. Quick W1 path:

```bash
export WORLDSMITHS_ENGINE_ROOT=/path/to/worldsmiths   # if not ../worldsmiths
./gradlew :desktop:run
```

1. Confirm the board renders under pitch (38° ortho), not flat AABB.
2. Place mirrors from the tray on empty cells; confirm goal pads (entry white / exit colored).
3. Tap a port strip for **one** Test Fire — emissive beam on the merged board (no Ghost layer).
4. Spend remaining mirrors, then **Enter** / **V** to Verify.

Done when steps 2–4 succeed under pitch. See playbook Part 6 for the full V8 checklist including fixedPieces lock mark.
