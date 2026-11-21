# Strategic Playbook — Placement Patterns & Deduction

Working title **Refract** (`GDD.md`); repo/product tree **Prismatrix**.

This file is an author-facing pattern library for later **puzzle levels** and **tutorials**. It is derived from classic grid-raytracing deduction (Black Box, Khet, Laser Maze) and checked against Prismatrix’s shared optics (`TraceCore`) plus mode rules in `GDD.md`.

**Default board in examples:** Classic PvP / mid-campaign `8×8` (`(0,0)…(7,7)`), ports `T0..T7`, `B*`, `L*`, `R*`.  
**Classic piece pool:** 3 orbs (R/G/B) + 4 mirrors (`/` `\`) = 7 pieces.  
**Puzzle W1 today:** `6×6`, mirrors only, untyped `mirrorBudget` — see World tags below.

---

## 0. How to use this when writing puzzles / tutorials

| Goal | How to use a pattern |
| --- | --- |
| **Tutorial beat** | Teach **one** rule or theorem per level. Prefer patterns tagged *Teach*. Pair with 1–3 clues that isolate the idea. |
| **Puzzle difficulty** | Stack *Ambiguity* patterns (Chameleon, Periscope, Staircase) and cut easy eliminations (deny free straight-throughs). |
| **Clue design** | Author the secret board first, fire Trace for candidate ports, pin 5–14 clues that leave a unique (or intended near-unique) inventory solution. |
| **PvP setup meta** | Defensive patterns (§1) are opponent-facing; offensive patterns (§2) are solver-facing — tutorials can teach §2 in World order. |
| **Sanity check** | Before shipping coords, re-Trace the layout. Mirror orientation mistakes flip exits (see §4 Errata). |

**World applicability tags**

| Tag | Meaning |
| --- | --- |
| W1 | Reflection — `6×6`, mirrors only |
| W2 | Refraction — mirrors + primary orbs |
| W3 | Prismatics — full RGB + Splitter/Absorber (GDD) |
| W4 | Restricted sensors — Port-only / Color-only clues |
| W5 | Grandmaster — `10×10`, multi-splitter |
| PvP | Versus setup / scan meta (Classic unless noted) |
| Chaos | Splitter / Absorber mutators (GDD; Splitter **not** in M1 `PieceType` yet) |

---

## 0.1 Canonical rules (verify against these, not memory)

Source of truth for implemented Trace: `optics/TraceCore.java` + M1 HLD Appendix A. Product modes: `GDD.md` §2–3.

### Ports & axes

- `T{i}` → cell `(i, 0)` traveling **Down**; `B{i}` → `(i, N-1)` **Up**; `L{i}` → `(0, i)` **Right**; `R{i}` → `(N-1, i)` **Left**.
- `y = 0` is the **top** row (under the T ports).

### Mirrors

| Piece | Reflections |
| --- | --- |
| `/` (FORWARD) | Right↔Up, Left↔Down |
| `\` (BACK) | Right↔Down, Left↔Up |

### Orbs & color (3-bit RGB)

- Beam starts **Neutral White** `0` (`000`).
- On an orb cell: `color |= orbMask` (**immediate** on that cell — locked in Trace; supersedes GDD’s older “next cell” wording for deduction).
- Masks: R=`1`, G=`2`, B=`4` → Yellow=`3`, Magenta=`5`, Cyan=`6`, **Prismatic**=`7`.
- Idempotent: `Red | Red = Red`. Hitting the same primary twice does not change the exit mask.
- Mixing is **cumulative / additive only** (never subtractive).

### Other pieces

- **Absorber (`X`):** `exitPort = null` (implemented).
- **Splitter (`Y`) / Filters:** GDD mutators; **not** in M1 piece set — treat Chaos patterns as design-forward until Trace grows.
- **Cycle:** revisiting `(x, y, dir, color)` → null exit (no throw).
- **One piece per cell.**

### Mode facts used below

- PvP Classic inventory: **3 orbs + 4 mirrors**.
- Puzzle loop: empty grid + fixed inventory + border clues → place all → **Verify**.
- Sandbox (GDD §4): Ghost pieces, Test Fire, Green/Red/Neutral consistency badges — design tutorials around this even if UI lands later.

---

## Part 1 — Defensive placement patterns (PvP setup & hard layouts)

**Goal:** Maximize path ambiguity, disguise orb coordinates, force extra triangulation shots.

### 1. The "Chameleon" Double-Orb Line *(Color Masking)* — W2+ · PvP · Teach(secondary)

**Setup:** Two different primary orbs on one corridor (same row or column), with mirrors optional so one beam can traverse both before exit.

**Verified sketch (`8×8`):** `ORB_R (2,2)`, `ORB_B (2,5)`, `/ (2,6)` → `T2 → L6` with **Magenta (`5`)**.

**Why it works:** Opponent sees one secondary exit color. They know both bits were set, not order or where along a multi-bounce path each orb sat.

**Author note:** Great W2→W3 bridge tutorial: “one exit color can encode multiple orbs.”

---

### 2. The "Prismatic" Triple-Threat Corridor — W3 · PvP

**Setup:** Route one beam through all three orbs (R→G→B in any order), often with 1–2 mirrors to lengthen the path.

**Why it works:** Exit **Prismatic (`7`)** proves all three primaries were touched, but a long zig-zag yields many valid orb permutations along the route.

**Author note:** Prefer as a *late* campaign clue, not a first Prismatic teach — too many degrees of freedom unless other clues pin geometry.

---

### 3. The "Phantom Empty" Edge Decoy — W1+ · PvP

**Setup:** Leave the outer ring empty (`row 0`, `row N-1`, `col 0`, `col N-1`); cluster all pieces in the inner `(N-2)×(N-2)`.

**Why it works:** Perimeter ports often return Neutral straight-throughs. Players who assume “spread pieces” burn early scans on empty edges.

**Author note (W1 `6×6`):** Same idea with ring `0`/`5`. For teaching, *invert* it once: put a single mirror on the ring so the decoy fails and players learn not to overfit.

---

### 4. The "U-Turn" Perimeter Trap — W1+ · PvP · Teach(2-bounce)

**Setup (verified):** `\ (2,1)` and `/ (3,1)`.

**Trace:** `T2 → T3`, Neutral, **2 bounces**.

**Why it works:** Entry/exit look like a shallow adjacent-port hop. Depth (`y=1` vs deeper U-turns) stays ambiguous until side shots.

**⚠ Draft correction:** `/ (2,1)` + `\ (3,1)` does **not** U-turn to `T3` — Trace gives `T2 → L1` (1 bounce). Always place the first mirror so **Down→Right** (`\`) when sending `T{i} → T{i+1}`.

---

### 5. The "Periscope" Offset — W1+ · PvP · Teach(2-bounce displacement)

**Setup (verified):** `\ (2,2)` and `\ (5,2)` (same row `y=2`).

**Trace:** `T2 → B5`, Neutral, **2 bounces**.

**Why it works:** Exit looks like a clean column shift (`2→5`), tempting guesses on the center diagonal instead of the true bounce row.

**⚠ Draft correction:** Two `/` on `(2,2)` and `(5,5)` yields `T2 → L2`, not `B5`. Periscope needs matching turn geometry on a **shared corridor**, not a visual diagonal.

---

### 6. The "Orb Shield" (Mirror-Guarded Orb) — W2+ · PvP

**Setup (verified):** `ORB_R (4,4)`, `/ (4,3)`, `\ (3,4)`.

| Shot | Result |
| --- | --- |
| `T4` | Deflected **before** orb → `L3` White |
| `L4` | Deflected **before** orb → `B3` White |
| `B4` | Hits orb, then `/` → colored exit (`R3`, Red) |
| `R4` | Hits orb, then `\`` → colored exit (`T3`, Red) |

**Why it works:** Cuts discovery angles: Top/Left look empty; Bottom/Right reveal color but still warp geometry.

**Author note:** Teach shields only after players trust straight-through elimination (§13–14), or they misread White deflections as “empty column.”

---

### 7. The "False Symmetry" Split — W1+ · PvP

**Setup:** Symmetric / tidy mirror cluster on one half; all orbs (or remaining mirrors) tight in one opposite corner.

**Why it works:** Humans expect balanced boards. After mapping the “pretty” half, they probe the other half sparsely instead of sweeping a `3×3`.

**Author note:** Excellent puzzle *flavor* without needing new mechanics — W1-friendly.

---

### 8. The "Color Shadow" (Redundant Orb Hit) — W2+ · PvP · Teach(`|=` idempotent)

**Rule:** `primary | primary = primary`. A path that crosses the same orb twice exits like a single hit.

**Verified sketch (`8×8`, 3 mirrors):**  
`ORB_R (2,3)`, `\ (2,6)`, `/ (4,6)`, `\ (4,3)`  
→ `T2` crosses `(2,3)` downward, returns leftward through `(2,3)`, exits colored **Red** (still mask `1`).

**Why it works:** Disguises loop/re-entry structure; bounce parity and ports look “too complex” for the simple color.

**⚠ Draft correction:** The earlier `\ (2,5)` + `/ (4,5)` sketch never re-hit the orb (`T2 → L5` after one bounce). Return path must leave column `2` and re-enter it from the opposite direction **without** blocking the return with the outbound mirrors.

---

### 9. The "Corner Pocket" Pin — W1+ · PvP · Teach(port reach)

**Setup:** Piece in a literal corner, e.g. orb or mirror at `(0,0)` or `(N-1,N-1)`. Optional diagonal neighbor at `(1,1)`.

**Why it works:** Only `T0` and `L0` enter `(0,0)` as first cell. Interior probes never see a pure corner occupant unless a bounce routes onto it.

**Author note:** Strong W1 teach for “ports address cells.” Keep inventory tiny.

---

### 10. The "Staircase" Cascade — W1+ · PvP · Teach(multi-bounce)

**Setup (verified):** `\ (0,1)`, `\ (2,1)`, `\ (2,3)`, `\ (4,3)`.

**Trace:** `T0 → B4`, Neutral, **4 bounces** (uses full Classic mirror count).

**Why it works:** Large entry→exit displacement; manual mental tracing fails without Sandbox Test Fire.

**Author note:** W1 end-of-world boss energy. Clue set should include at least one cross-axis shot so the staircase isn’t underdetermined.

---

### 11. The "Bait & Switch" Solo Orb — W2+ · PvP

**Setup:** One orb exposed near center (e.g. `(3,3)`) with no local mirrors; hide the other two behind a dense edge maze.

**Why it works:** Fast center find creates false confidence; opponent sprays nearby empties while the real cluster sits on the rim.

**Author note:** In puzzles, mirror this as a *red-herring clue*: an easy colored straight-through that is not sufficient to place the rest.

---

## Part 2 — Offensive deduction (PvP scans, Puzzle solve, tutorial order)

**Goal:** Resolve boards in few shots (PvP often ~6–9) or with a tight clue set (Puzzle).

Suggested **tutorial order:** §13 → §15 → §4/§5 → §14 → §16 → §17 → §18 → (§19 when Splitter ships).

### 12. The "Checkerboard" Parity Opening — W1+ · PvP · Teach(opening)

**Strategy:** Early game, do not fire adjacent ports (`T2` then `T3`). Sample every 2nd/3rd lane on perpendicular axes (e.g. `T1`, `T5`, `L2`, `L6`).

**Why it works:** Mirrors throw beams onto perpendicular axes; spaced probes separate empty corridors from clusters faster than neighbor walks.

---

### 13. The "Straight-Through" Elimination Rule — W1+ · Teach(core) · Puzzle staple

**Rule:** If `T3 → B3` (or `L3 → R3`) as **Neutral White**, every cell on that line is empty of **mirrors and orbs** (mirrors would bend; orbs would color).

**Application:** Mark the entire line clear. On `8×8`, one shot clears `8/64 = 12.5%` of cells.

**W1 note:** Color is always `0` today — White straight-through still clears mirrors on that line.

---

### 14. The "Straight-Through Colored" Pinpoint — W2+ · Teach(orb pin)

**Rule:** If `T3 → B3` as **Red** (mask `1`):

1. **Zero mirrors** on column 3 (else the beam would exit a non-opposite port).
2. **At least one Red contribution** on that column (orb R somewhere on the line; G/B absent on that path).

**Application:** Fire a perpendicular probe. Intersections of “must contain R” lines vs cleared lines pin candidates. Full unique pin often needs one more consistent cross-shot or Sandbox Ghost check.

---

### 15. The "Odd-Bounce" Axis Theorem — W1+ · Teach(parity) · PvP Classic

**Rule (standard mirrors only):**

| Entry side family | Exit side family | Mirror count on path |
| --- | --- | --- |
| T/B | L/R | **Odd** (1 or 3 …) |
| T/B | T/B (not straight-through) | **Even** (2 or 4 …) |
| Straight-through opposite | — | **0** mirrors on that line |

**Classic caveat:** With only **4** mirrors total, a lone unexplained `T → L` often collapses to a **single** bounce at the entry-column × exit-row intersection **if** other mirrors are already accounted for. If three mirrors remain free, odd parity alone is not a unique pin — say so in tutorials.

---

### 16. The "Color Ordering" Causality Check — W2+ · Teach(subsets)

**Rule:** Exit masks are bitwise OR along the path. If path A exits Red and path B exits Yellow (`R|G`), any cell on A cannot hold the only Green unless A would also show Green.

**Application:** When two clue paths cross, use color **subsets** to delete impossible orb placements at the intersection instantly.

---

### 17. The "Time-Reversal" Back-Trace — Puzzle staple · Teach(Sandbox)

**Rule:** Mirror geometry is reversible; color accumulation is path-directional (OR still depends on which orbs were visited).

**Application:** From a clue `T2 → R5 [Yellow]`, work backward from `R5` in Sandbox. Candidate 1–2 mirror joints between column `2` and row `5` form a short list; R and G must lie on that connected path.

---

### 18. The "Sandbox Conflict" Binary Search — PvP · Puzzle · Teach(hypothesis)

**Strategy:** Place Ghost pieces until all known shots show Green. Find two Ghost layouts A/B that both fit. Fire (or, in Puzzle, prefer a clue) through a port where A and B **disagree** on `{exitPort, colorMask}`.

**Application:** One result eliminates half the remaining hypothesis space — author daily/hard puzzles so the intended solution is the unique survivor under the clue set.

---

### 19. The "Splitter Fork" Triangulation — W3 · Chaos · design-forward

**GDD rule:** Splitter (`Y`) emits two beams opposite along the perpendicular axis → two exits (when Trace supports multi-exit / dual results).

**Application:** Shared row/column of the two exits (absent further mirrors) points at the Splitter’s line.

**Status:** Not in M1 `PieceType` / singular-exit Trace. Keep for W3+ content specs; do not ship W1–W2 levels that require it.

---

### 20. The "1-Turn Gamble" EV Threshold — PvP endgame only

**Strategy:** In timed/race PvP, don’t always wait for mathematical certainty.

**Application (Orbs Only wincon):** If two orbs are pinned and the third is 50/50, **Submit Guess** when the opponent is ~1 solve-shot away — EV of a 50% win can beat giving them the tempo.

**Penalty reminder (GDD):** Wrong guess forfeits the turn and grants the opponent a bonus scan — teach the risk explicitly.

---

## Part 3 — Pattern index for content pipelines

| # | Pattern | Best for | Worlds |
| --- | --- | --- | --- |
| 1 | Chameleon | Secondary-color teach; ambiguity | W2+ PvP |
| 2 | Prismatic corridor | Late ambiguity | W3 PvP |
| 3 | Phantom Empty | Opening tempo tax | W1+ PvP |
| 4 | U-Turn | 2-bounce teach | W1+ |
| 5 | Periscope | Displacement teach | W1+ |
| 6 | Orb Shield | Angle restriction | W2+ |
| 7 | False Symmetry | Soft psychology / layout | W1+ |
| 8 | Color Shadow | `\|=` idempotent | W2+ |
| 9 | Corner Pocket | Port addressing | W1+ |
| 10 | Staircase | Multi-bounce boss | W1+ |
| 11 | Bait & Switch | Red-herring | W2+ |
| 12 | Checkerboard opening | Scan habit | W1+ PvP |
| 13 | Straight-through clear | Core elimination | W1+ |
| 14 | Straight colored pin | Orb locate | W2+ |
| 15 | Odd-bounce theorem | Parity | W1+ |
| 16 | Color causality | Intersection logic | W2+ |
| 17 | Time-reversal | Puzzle method | All puzzle |
| 18 | Sandbox binary search | Hypothesis control | PvP / hard puzzle |
| 19 | Splitter fork | Chaos triangulation | W3+ / Chaos |
| 20 | EV guess | Ranked endgame | PvP |

*(Original brainstorm listed 22 items; defensive 1–11 + offensive 12–20 = 20 named patterns. Extra “items” were setup variants, not separate rules.)*

---

## Part 4 — Verification errata (draft → Trace)

Checked with headless `TraceCore` on `8×8` (2026-11-21):

| Claim | Verdict |
| --- | --- |
| Classic 3 orbs + 4 mirrors; RGB OR masks; Prismatic `7` | **OK** (`GDD` + `PieceType`) |
| Straight White ⇒ empty line; Straight colored ⇒ no mirrors + color on line | **OK** |
| Odd/even bounce axis parity | **OK** (mirrors only) |
| U-Turn `/ (2,1)` + `\ (3,1)` ⇒ `T2→T3` | **FAIL** → use `\ (2,1)` + `/ (3,1)` |
| Periscope `/ (2,2)` + `/ (5,5)` ⇒ `T2→B5` | **FAIL** → use `\ (2,2)` + `\ (5,2)` |
| Orb Shield blocks T/L, allows B/R color | **OK** (B/R still bounce after the orb) |
| Color Shadow double-hit with draft coords | **FAIL** → use leave-and-return path in §8 |
| Splitter dual-exit triangulation | **GDD-only** until Trace supports Splitter |
| GDD “orb color on next cell” | **Superseded** by Trace immediate `color \|=` on orb cell for deduction |
| Outer-ring Phantom Empty / Corner Pocket intuition | **OK** under port entry rules |

---

## Part 5 — Mini authoring checklist

1. Pick **one teach target** (or one ambiguity target for hard puzzles).
2. Choose board size + inventory from World / mutator table.
3. Place pieces using a pattern from §1 (optional).
4. Trace candidate ports; select clues that encode the teach target without soft-locking.
5. Confirm uniqueness (or intentional multi-solution) via Ghost / alternate layouts (§18).
6. Tag the level with pattern IDs (`U-Turn`, `§13`, …) for later tutorial cross-links.
7. Re-Trace after any mirror orientation edit — `/` vs `\` is the most common silent break.
