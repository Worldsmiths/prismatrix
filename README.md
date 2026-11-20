# Prismatrix

Outside-consumer Worldsmiths game (includeBuild of unpublished engine). **This repo is M0 scaffold only — not playable.** First playable target is Puzzle W1 at M2. Product intent lives in [`GDD.md`](GDD.md). The in-engine canary remains `samples/relay` in Worldsmiths; do not treat this tree as that canary.

## Bar C

- No direct `com.badlogicgames.gdx` dependency in `:game`
- No `com.badlogic.gdx` imports in game sources
- Transitive LibGDX via `ws-core` is OK

Enforce locally with `ci/assert-bar-c.sh` (CI will wire this in a later task).

## Requirements

- JDK 21+ to run Gradle
- A checkout of [Worldsmiths/worldsmiths](https://github.com/Worldsmiths/worldsmiths) (engine)
- Game module compiles for **Java 8** (`options.release = 8`)

## Engine path

| Context | Path |
| --- | --- |
| Local default | Sibling `../worldsmiths` |
| Override | `WORLDSMITHS_ENGINE_ROOT` or `-Dworldsmiths.engineRoot=...` |
| CI | Dual-checkout at `engine/` + `game/`; pin SHA in `.github/workflows/ci.yml` (when present) |

Uses bare `includeBuild` against the engine checkout (no `dependencySubstitution`).

## Local build

```bash
export WORLDSMITHS_ENGINE_ROOT=/path/to/worldsmiths   # if not ../worldsmiths
./gradlew :desktop:compileJava :game:test
```

## Run desktop (manual)

```bash
./gradlew :desktop:run
```

Blank window titled **Prismatrix** — scaffold boot only, not a playable mode.
