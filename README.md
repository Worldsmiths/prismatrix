# Prismatrix

Outside-consumer Worldsmiths game (includeBuild of unpublished engine). **Trace kernel landed (M1 Optics); still not playable — Puzzle W1 @ M2.** Product intent lives in [`GDD.md`](GDD.md). The in-engine canary remains `samples/relay` in Worldsmiths; do not treat this tree as that canary.

## Bar C

- No direct `com.badlogicgames.gdx` dependency in `:game`
- No `com.badlogic.gdx` imports in game sources
- Transitive LibGDX via `ws-core` is OK

Enforce locally with `ci/assert-bar-c.sh`; CI runs the same gate on every push/PR.

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

## Run desktop (manual)

```bash
./gradlew :desktop:run
```

Blank window titled **Prismatrix** — Trace is not wired into the game loop yet; scaffold boot only, not a playable mode.
