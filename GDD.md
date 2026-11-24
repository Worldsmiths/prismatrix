# MASTER GAME DESIGN DOCUMENT (GDD v2.0)

## 1. Game Identity & Vision

* **Working Title:** *Refract*
* **Genre:** Spatial Optical Deduction / Tactical 1v1 / Incremental Engine-Builder
* **Target Platforms:** iOS, Android, Web / PC
* **Visual Style:** **2.5D Tilted Orthographic "Tactile Optical Lab"** — matte dark slate grid (`#12141D`), physical beveled glass/metal pieces, HDR neon laser beams with real-time floor bounce lighting, and procedural pentatonic audio synthesis.

---

## 2. Core Physics & Grid Mechanics (Shared Ruleset)

### 2.1 Grid & Emitter Ports

* An $N \times N$ interior grid (default $8 \times 8$, coordinates `(0,0)` to `(N-1, N-1)`).
* Surrounded by $4N$ border emitter/detector ports:
* **Top (`T0..Tn`):** Fires Down `(0, +1)`
* **Bottom (`B0..Bn`):** Fires Up `(0, -1)`
* **Left (`L0..Ln`):** Fires Right `(+1, 0)`
* **Right (`R0..Rn`):** Fires Left `(-1, 0)`



### 2.2 Optical Components

* **Forward Mirror (`/`):** Reflects 90° (`Right↔Up`, `Left↔Down`).
* **Backslash Mirror (`\`):** Reflects 90° (`Right↔Down`, `Left↔Up`).
* **Translucent Orbs (`R`, `G`, `B`):** Beam passes straight through; applies additive color starting on the **next** cell.
* **Mutator / Advanced Components:**
* **Splitter (`Y`):** Splits 1 incoming beam into 2 perpendicular beams (left and right of incoming vector).
* **Absorber (`X`):** Terminates the beam (`exitPort: null`).
* **Color Filter (`F_R`, `F_G`, `F_B`):** Passes beams containing the matching color bit; reflects non-matching beams 180°.



### 2.3 Color Mixing Model (3-Bit RGB Bitmask)

Beams enter as **Neutral White (`000` / `0`)**. Hitting an orb applies `newColor = currentColor | orbMask`.

* `0 (000)`: Neutral White (0 orbs hit)
* `1 (001)`: Red | `2 (010)`: Green | `4 (100)`: Blue
* `3 (011)`: Yellow (R+G) | `5 (101)`: Magenta (R+B) | `6 (110)`: Cyan (G+B)
* `7 (111)`: **Prismatic** (R+G+B — visually distinct gold/iridescent beam)

---

## 3. Game Modes & Progression

### Mode 1: 1v1 Tactical PvP (Versus)

* **Core Loop:**
1. **Setup Phase (30s):** Secretly place inventory pieces on your grid.
2. **Turn Phase:** Alternate turns choosing **Fire Beam** (select border port $\rightarrow$ receive `{ exitPort, exitColor }`) or **Submit Guess** (lock in opponent's piece positions).
3. **Penalty:** Incorrect guess forfeits the current turn and grants the opponent 1 bonus scan.


* **Connectivity Modes:**
* **Solo vs. AI:** 4 difficulty tiers (Novice, Tactician, Strategist, Master Constraint-Solver).
* **Local P2P (Offline):** Bluetooth Low Energy / Wi-Fi Direct (`Nearby Connections` / `MultipeerConnectivity`).
* **Global Online Wi-Fi:** Ranked ELO matchmaking (Classic Preset) + Custom Private Rooms (4-character invite code).


* **Custom Match Mutators (Lobby Host Config):**
* **Board Size:** `6x6` (Blitz), `8x8` (Classic), `10x10` (Grand).
* **Win Condition:** `Orbs Only` vs. `Orbs + Mirrors` (exact orientation).
* **Piece Pool:** `Classic (3 Orbs, 4 Mirrors)` vs. `Chaos (+2 Splitters, +1 Absorber)`.
* **Clue Visibility:** `Full (Port + Color)`, `Geometry Only (Port Only)`, `Spectrum Only (Color Only)`.
* **History Log:** `Full Log`, `Last 3 Shots`, `Blind (No History — Sandbox notes only)`.
* **Shot Limit:** `Unlimited` vs. `15-Shot Cap`.



### Mode 2: Puzzle Mode ("Optic Trials")

* **Core Loop:** Single-player construction / routing puzzles on a live board.
  1. Load a level with paired **goals** (entry port → `{exitPort, colorMask}`), a player **inventory** (`mirrorBudget` plus optional `orb1`/`orb2`/`orb3`), and optional **fixed décor**.
  2. Place mirrors (`/` `\`) and any orbs from the tray onto free cells; fixed pieces are already seeded, recessed with a lock mark, and cannot be moved or spent from the player budget.
  3. **Test Fire** any border port to preview a beam on the merged board (fixed + player). Goal pads stay visible: entry ports white, exit ports tinted to the goal color.
  4. Spend the full player inventory, then press **Verify** (Enter / V). Verify succeeds only when every goal matches a Trace on the merge; leftover mirrors or orbs refuse Verify (not a FAIL).
* **No Ghost / badge / Promote ritual.** Puzzle does not teach provisional Ghost pieces or Green/Red/Neutral consistency badges; place on the live board and Verify explicitly.
* **Campaign Structure (120 Levels across 5 Worlds):**
* **World 1 — Reflection:** $6 \times 6$, mirrors only (teaches geometry). Packaged W1 omits orbs / `orbColors`.
* **World 2 — Refraction:** $6 \times 6$ & $8 \times 8$, mirrors + primary orbs (`orbColors` required when any orb count &gt; 0).
* **World 3 — Prismatics:** Full RGB additive mixing + Splitters/Absorbers.
* **World 4 — Restricted Sensors:** Uses PvP Mutators (`Port Only` goals, `Color Only` goals).
* **World 5 — Grandmaster Vaults:** $10 \times 10$ complex multi-splitter deduction.


* **Level JSON (author contract):** Required `goals[{entryPort, exitPort, colorMask}]`. Optional `orb1`/`orb2`/`orb3` (default 0); when any orb &gt; 0, `orbColors` is required (`RED_YELLOW_BLUE` or `RED_GREEN_BLUE`). Optional `fixedPieces[{x,y,type}]` with types `/`, `\`, or `ORB_1`/`ORB_2`/`ORB_3` (fail-closed: in-bounds, no duplicates). Oracles (test/packaged solutions) place **player** pieces only after the fixed seed.
* **Daily Seed:** One procedurally generated global puzzle every 24 hours with a time-based leaderboard.

### Mode 3: Incremental Mode ("Lumen Factory")

* **Core Loop:** Build a self-sustaining optical bounce factory on an expandable grid ($4 \times 4 \rightarrow 12 \times 12$). Auto-Emitters pulse White beams that bounce off Mirrors, pass through Orbs/Amplifiers, and exit into border **Collector Ports** to generate **Lumens**.
* **Currency Formula:**

$$\text{Lumens} = (\text{Tiles} \times \text{ColorMult} \times 1.15^{\text{Bounces}}) \times \text{CollectorMult}$$


* **Hybrid Upgrade System:**
1. **Spatial Piece Shop (Spend Lumens):** Buy and drag-place Mirrors, Orbs, Splitters, Amplifier Floor Tiles ($+20\%$ per pass), and Loop Capacitors (store splitter-loop energy for burst payouts). Merging two identical Lv.1 pieces creates a Lv.2 piece ($2.5\text{x}$ multiplier + bonus effect).
2. **Prism Constellation Skill Tree (Spend Spectrum Shards):** Earned via Lumen milestones and Prestige ("Wavelength Shifts"). Unlocks global rules: *Auto-Merge*, *Emitter Fire Rate*, *Multi-Wall Emitters*, *Beam Duplication Chance*, and *Collector Port Overclocking*.


* **Final Win Condition — "The Singularity Core":**
* At $12 \times 12$ grid expansion and Tier-4 Skill Tree unlock, a **4-Tile Singularity Core** spawns at the grid center `(5..6, 5..6)`.
* Each of the 4 faces requires a sustained simultaneous beam threshold (e.g., North: *Prismatic @ 50M/s*, East: *Cyan @ 25M/s*, South: *Magenta @ 25M/s*, West: *Yellow @ 25M/s*).
* Engineering a layout that satisfies all 4 faces charges the Core from `0%` to `100%` over 10 seconds, triggering the **Victory Finale** (run stats, speedrun time, and optional **Endless Overdrive Mode**).



---

## 4. Puzzle Play UI (Mode 2)

* **Tray:** FORWARD (`/`) and BACK (`\`), plus only orb slots whose level count is &gt; 0 (tinted from the level `orbColors` palette when present).
* **HUD:** Remaining player mirrors/orbs, compact goals list, and Verify affordance (disabled until inventory is spent).
* **Board:** Persistent goal pads (entry white / exit colored); recessed fixed pieces with a lock mark; player pieces paint on free cells over the fixed seed.
* **Test Fire:** Tap a border port to sticky-preview a Trace on the **merged** board (fixed + player). Does not append to any PvP observation log.
* **Verify:** Enter / V when remaining mirrors and orbs are both 0; OK / FAIL / refused as in Mode 2 core loop.

PvP Versus keeps its own Fire / Guess / history chrome; it does **not** share a Ghost-board or consistency-badge layer with Puzzle.



---

## 5. Art & Audio Direction Spec (Solo-Dev Pipeline)

* **Camera:** 3D engine using an **Orthographic Camera** pitched at **40° X-axis, 45° Y-axis** (isometric diamond) or **38° X-axis, 0° Y-axis** (clean top-angled tactical view — recommended for easier mobile port tapping).
* **Materials & Shaders:**
* Zero complex textures required. Use PBR materials with high roughness (`0.85`) on the dark slate board (`#12141D`) and metallic/glass shaders on pieces.
* Laser beams rendered via volumetric `LineRenderer` / `MeshStrip` with **HDR Emission (`intensity: 2.5`)** + global **Bloom post-processing** + dynamic point lights tracking beam tips.


* **Procedural Audio:**
* Pentatonic scale synth (`C4, D4, E4, G4, A4, C5, D5, E5`). Each consecutive mirror bounce in a single laser trace increments the pitch index by `+1`.
