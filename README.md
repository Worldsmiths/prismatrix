# Prismatrix

Outside-consumer Worldsmiths game (includeBuild of unpublished engine). **Playable for Puzzle World 1** on desktop: load packaged W1 levels, place `/` and `\` mirrors against an untyped budget, and Verify against Trace. Product intent lives in [`GDD.md`](GDD.md). The in-engine canary remains `samples/relay` in Worldsmiths; do not treat this tree as that canary.

## Play Puzzle W1

```bash
export WORLDSMITHS_ENGINE_ROOT=/path/to/worldsmiths   # if not ../worldsmiths
./gradlew :desktop:run
```

Loads `levels/w1/01.json`. Tap the tray `/` or `\` then an empty cell to place; tap a filled cell to return a mirror; press **Enter** or **V** when remaining budget is 0 to Verify.

## Thin3D P1 (M3 — not required for M2)

Pitched / Ghost UX needs Thin3D **P1** (ortho + pick-ray + emissive + TRS) in the engine. That work is named for **M3** and is **not** required for this M2 first playable. M2 uses flat Draw2D + Font + Input only; no Thin3D ortho/pick/emissive/TRS calls in M2 sources.

**Temporary name fence:** `ci/assert-thin3d-p1-names.sh` fails if `game/src` Java mentions P1 API names (`OrthographicCamera`, `getPickRay`, `create*Emissive`, `setTranslationRotationScaling`, `createOrthographicCamera`). **Removable** with the first intentional Thin3D P1 call PR (together with a `WORLDSMITHS_ENGINE_REF` pin bump). CI runs it next to Bar C.

## Bar C

- No direct `com.badlogicgames.gdx` dependency in `:game`
- No `com.badlogic.gdx` imports in game sources
- Transitive LibGDX via `ws-core` is OK

Enforce locally with `ci/assert-bar-c.sh`; CI runs the same gate on every push/PR.

## CI shape

CI stays **Bar C** + Thin3D P1 name fence + `:desktop:compileJava` + `:game:test`. There is **no** headed `:desktop:run` in CI.

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
