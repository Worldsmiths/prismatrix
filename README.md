# Prismatrix

Outside-consumer Worldsmiths game (includeBuild of unpublished engine). **Playable for Puzzle World 1** on desktop: load packaged W1 levels, place `/` and `\` mirrors against an untyped budget, and Verify against Trace. Product intent lives in [`GDD.md`](GDD.md). The in-engine canary remains `samples/relay` in Worldsmiths; do not treat this tree as that canary.

## Play Puzzle W1

```bash
export WORLDSMITHS_ENGINE_ROOT=/path/to/worldsmiths   # if not ../worldsmiths
./gradlew :desktop:run
```

Loads `levels/w1/01.json`. Tap the tray `/` or `\` then an empty cell to place; tap a filled cell to return a mirror; press **Enter** or **V** when remaining budget is 0 to Verify.

## Thin3D P1 (M3)

Pitched / Ghost UX consumes Thin3D **P1** (ortho + pick-ray + emissive + TRS). CI pins a Worldsmiths SHA that includes P1 (`WORLDSMITHS_ENGINE_REF` in `.github/workflows/ci.yml`). The temporary P1 name fence used before that pin is gone; game sources may call those APIs.

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

## Soft sandbox re-eval bench (optional)

HLD soft NFR: sandbox consistency re-eval p95 &lt; 0.2 ms for K≈20 observations (6×6 ghosts). Showcase target only — prints p50/p95/max and fails only on an extreme multi-second wall timeout (peer of Trace latency bench).

```bash
./gradlew :game:test --tests io.github.worldsmiths.prismatrix.sandbox.SandboxReEvalBenchTest
```

Tagged `@Tag("bench")` in `SandboxReEvalBenchTest`; default `:game:test` still runs it.

## Founder headed smoke checklist (manual — not CI)

Run once after pitched UI lands. **Not** a CI gate; sign off outside automation.

```bash
export WORLDSMITHS_ENGINE_ROOT=/path/to/worldsmiths   # if not ../worldsmiths
./gradlew :desktop:run
```

1. Confirm the board renders under pitch (38° ortho), not flat AABB.
2. In Ghost mode, pick **one** empty cell and place a ghost mirror.
3. Tap a port strip for **one** Test Fire — emissive beam along the ghost Trace path.
4. (Optional) Promote / Verify chrome still responds; refuse messages if over budget.

Done when steps 2–3 succeed under pitch.
