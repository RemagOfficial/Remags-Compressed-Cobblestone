# RCC — Master Plan: Road to a Googol

**What this file is.** A living plan and progress tracker for *Remag's Compressed Cobblestone* from where we are today to the end of the mod.
It is **not** implementation instructions. Nothing here is authorised work; each band is built only when the user says so, one band at a time,
and each build is verified by the user in game.

**Where we are:** Tier 0 and Tier 1 are complete. The ladder currently tops out at a **tier-10^12 block**. The goal is **8–10 bands** ending at
**tier 10^100**.

---

## 1. Unit of measure (read this first)

Everything in the mod is measured in **compression tiers**, and a tier-`t` block is worth `9^t` base-cobblestone-equivalent (BCE).

| Unit | Tier 0 ceiling | Tier 1 ceiling (today) | Goal band ceiling |
| --- | --- | --- | --- |
| **Tier number** | `10^3` (1000) | `10^12` | `10^100` |
| BCE mass that block is worth | `10^954` | `10^(9.5×10^11)` | `10^(9.5×10^99)` |

The goal is stated as **tier `10^100`**, because that is the unit every existing constant uses
(`MANUAL_MAX_LEVEL = 1000`, `MANIPULATOR_MAX_OUT = 10^12`). Note the consequence: as a *BCE mass*, a googol of cobblestone
(`10^100` BCE) is only **tier ~105** — which the tier-0 band already passes at tier 1000. So "the final block is worth 10^100 cobblestone"
would be a goal the mod blew past four bands ago, and it is not what this plan means. The ambition is in the **tier number**: the last
machine makes a block whose compression tier is `10^100`.

> **✅ D1 resolved:** the goal is **tier `10^100`** — the last machine's output block sits at compression tier `10^100`, worth `10^(9.5×10^99)` BCE.
> The BCE-multiplier reading was rejected exactly as expected: a googol of *mass* is only tier ~105, which band 0 already passes at tier 1000.

**Terminology used throughout:** *band* = one progression tier of machinery (what the player builds), *tier* also names a compressed block's
level. `Tier 2 band` = the second multiblock band; `tier-10^20 block` = a block at compression tier `10^20`.

---

## 2. The band ladder

Each band starts where the previous one ended: band `k`'s minimum input is band `k-1`'s maximum output
(the shipped rule: `MANIPULATOR_MIN_IN = 1000 = MANUAL_MAX_LEVEL`). Band widths grow, so the late bands feel longer — which is where
"long but not boring" comes from.

| Band | Status | Compressor shell | Decompressor shell | Generator tower | Max input tier | **Max output tier** | Band width (decades of tier) | Pipe tier | Storage tier |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | ✅ done | single block (Auto) | single block (Auto) | single block | `10^0` | `10^3` | 3 | I | I |
| 1 | ✅ done | 5×5×5 | 3×3×3 | 3×3×4 | `10^3` | `10^12` | 9 | II | II |
| 2 | ⏳ next | 7×7×7 | 5×5×5 *(fast)* | 5×5×6 | `10^12` | `10^20` | 8 | III | III |
| 3 | · | 9×9×9 | **7×7×7 ⚡ instant** | 7×7×8 | `10^20` | `10^28` | 8 | IV | IV |
| 4 | · | 11×11×11 | — *(none new)* | 9×9×10 | `10^28` | `10^38` | 10 | V | V |
| 5 | · | 13×13×13 | — *(none new)* | 11×11×12 | `10^38` | `10^50` | 12 | VI | VI |
| 6 | · | 15×15×15 | — *(none new)* | 13×13×14 | `10^50` | `10^64` | 14 | VII | VII |
| 7 | · | 17×17×17 | — *(none new)* | 15×15×16 | `10^64` | `10^80` | 16 | VIII | VIII |
| 8 | · final | 19×19×19 | — *(none new)* | 17×17×18 | `10^80` | **`10^100`** | 20 | IX | IX |

Totals: **9 bands** (2 shipped, 7 to build) — inside the 8–10 target. If you want 10, split band 8 into `10^80 → 10^90 → 10^100`.

**Size rules (one rule, no cap):** compressor side `= 2k + 3`, **decompressor side `= 2k + 1` for bands 1–3 only** (then the family stops), generator
tower `= (2k+1)` wide × `(2k+2)` tall. They reproduce every shipped structure exactly, and band `k`'s shell half-width is `k + 1` — which is what §4.12 is about.

| Band | Compressor side | Blocks to place | Decompressor side | Generator tower | Blocks to place | Shell half-width (port→core distance) |
| --- | --- | --- | --- | --- | --- | --- |
| 1 ✅ | 5 | 98 | 3 ✅ | 3×3×4 | 32 | 2 (today's fixed scan radius) |
| 2 | 7 | 218 | 5 | 5×5×6 | 96 | 3 |
| 3 | 9 | 386 | 7 ⚡ | 7×7×8 | 192 | 4 |
| 4 | 11 | 602 | — | 9×9×10 | 320 | 5 |
| 5 | 13 | 866 | — | 11×11×12 | 480 | 6 |
| 6 | 15 | 1178 | — | 13×13×14 | 672 | 7 |
| 7 | 17 | 1538 | — | 15×15×16 | 896 | 8 |
| 8 | 19 | **1946** | — | 17×17×18 | **1152** | 9 |

> **✅ D2 resolved: the multiblocks keep growing**, and the "that is a lot of masonry" problem is answered with **Patchouli's in-world multiblock
> preview** (§5) rather than by capping footprints. Consequences of that choice, all of them load-bearing:
> 1. **§4.12 is a hard blocker for band 2** — a port 3+ cells from its core cannot find it today, so bigger shells need the resolution fix *first*.
> 2. **§4.13** — rescan cost grows with shell volume (`19³` ≈ 6.9k cells per pass, and the generator tries two boxes); the periodic scan needs a
>    dirty-flag path.
> 3. **§4.14** — a 19- or 21-wide tower spans several chunks; unloaded cells currently un-form the machine, which becomes a real nuisance at scale.
> 4. Band 4+ also wants **more ports** (a 19×19×19 shell has 462 face-centre cells; one POWER port for 10⁵⁰-tier work is silly) — recommend required
>    port/interface counts that scale with the band, as part of the band table.

> **✅ D7 resolved: the decompressor family ends at band 3.** Every decompressor is already tier-agnostic — it takes any block and walks it toward a
> player-entered goal tier — so bands 4–8 need no new one; only speed distinguishes them, and speed is what band 3's **instant** machine (one action,
> straight down to the goal, whatever the input tier) settles.
>
> The speed issue is a **cliff, not a slope**, and it falls inside band 1. `DecompressorCoreBlockEntity` descends exactly `t.subtract(ONE)` per step at
> `DECOMP_MULTIBLOCK_TICKS_PER_STEP = 1`, i.e. **20 tiers per second**, `1.7×10^6` tiers per game-day, `6.3×10^8` per real year:
>
> | Descent from tier | Tiers to drop (to tier 0) | Real time at 20 tiers/s |
> | --- | --- | --- |
> | `10^3` (band 0 top) | 1,000 | 50 seconds |
> | `10^6` | 10⁶ | ~14 hours |
> | `10^9` | 10⁹ | **~1.6 years** |
> | `10^12` (band 1 top) | 10¹² | ~1,600 years |
> | `10^20` (band 2 top) | 10²⁰ | ~1.6×10¹¹ years |
> | `10^100` (goal) | 10¹⁰⁰ | ~1.6×10⁹¹ years |
>
> So a million tiers of descent is already a multi-day wait and a billion is longer than a save file's life. Consequences: **band 2's decompressor must
> not be a third 1-tier-per-tick machine** (it would be unbuildable-worthless — see §4.16 and D14), and band 3's instant one genuinely is the last one
> anyone will ever need, since "any tier, one action" has no headroom left to upgrade.

---

## 3. What every band ships (the repeating content list)

A band is complete when a player can **mint, move, store and compress** at its top tier, and **decompress** it — the last of those is satisfied by the
band-3 instant decompressor from band 4 onward, so it is the one deliverable that stops repeating. Per band:

- [ ] **Band constants** in `CompressionConfig` (min-in, max-out, cycle cost, ticks, **shell geometry: radius/height and required port counts**) — as a
   band-indexed table, not new names per band. This table is also what emits the Patchouli patterns (§5), so keep it complete enough to draw the shell.
- [ ] **Compressor multiblock core** (+ shell blocks; ports/frame/glass/interface are already shared).
- [ ] **Decompressor multiblock core** — *bands 2 and 3 only* (band 2 faster, band 3 instant); **not a band 4–8 deliverable** (§2/D7, §4.16).
- [ ] **Generator tower core** with its rung ladder (rung rule below).
- [ ] **Pipe tier** — one new `BcePipeTier` constant, ceiling = one max-out block's BCE.
- [ ] **Storage tier** — one new tank capacity (= one max-out block's yield **+ relative headroom**, see §4.5).
- [ ] **Recipes** for every new block, gated by the previous band's top block (§4.8 — placeholders are fine, *existing* is not optional).
- [ ] **Advancements**: one milestone per band chained onto the existing line.
- [ ] **Datagen**: models, blockstates, tags, loot (`copy_components`), lang keys — **plus the Patchouli book entry + multiblock pattern** for the band.
- [ ] **Patchouli**: one entry per machine family, each with a `patchouli:multiblock` page so the player can visualise the shell in world (§5).
- [ ] **Creative tab entries** (per-band group, see §7).
- [ ] **JEI**: info page per band machine.
- [ ] **Art**: ~6 textures per band (core, generator rung ramp, pipe, tank, frame variant). This is the real budget line, not the code.

Rough registration volume per band: ~2 cores + 8–12 generator rungs + 1 pipe + 1 tank + ~12 recipes + ~18 lang keys ≈ **25–30 entries** — except bands
4–8, which ship one core fewer (**~22–27 each**). Seven bands ≈ **170–200 entries**, nearly all of them variants of classes that already exist. Code is
cheap here; **art and gating numbers are the cost**, and stopping the decompressor ladder saves 5 core blocks, 5 models, ~5 texture sets and 5 GUI/menu
passes.

**Generator rung rule (generalised from what shipped):** rungs start at the previous band's ceiling `T_{k-1}` and end at **half** this band's
ceiling `T_k / 2`, stepping one order of magnitude at a time — that reproduces band 1 exactly (`10^3 … 5×10^11`, 10 rungs). Applied unchanged it
gives 21 rungs at band 8, so: **cap at 12 rungs per band**; when the span exceeds 12 decades, use `span / 12` log steps instead of whole decades.

---

## 4. Engineering infrastructure to build **once**, before band 2

These are the changes that make bands 3–8 copy-paste instead of re-authoring. Doing them during band 2 is the point of this section.

1. **Band-indexed constants.** Today the bands are individual named constants (`MANUAL_MAX_LEVEL`, `MANIPULATOR_MIN_IN`, `MANIPULATOR_MAX_OUT`,
   `GEN1_*`, `BCE_PIPE_MAX_TRANSFER_LOG[]`, `BCE_STORAGE_*_CAPACITY_LOG`). Nine bands need one table (`BANDS[k]`) that every machine, pipe, tank and
   generator reads, so adding a band is one row.
2. **One core class per machine type, parameterised by band.** The rung/`EnumProperty` pattern already works for generators and pipes; give the
   compressor/decompressor cores a `band` property (radius, shell rules and ceilings read from the table) rather than seven new block classes.
3. **Rung exponents must leave `long`.** `GeneratorCoreTier` stores exponents as `long` (max `9.2×10^18`). Band 5's ceiling is `10^50`. Migrate the
   rung exponent to the same BigInteger/string-backed form the compression tier already uses.
4. **Per-band tint normalisation.** `TINT_EXPONENT_CEILING = 12` normalises brightness across `tier 1 → 10^12`. From band 2 on, every block would
   render pure black. Rebase the ramp on the *player's current band* (or on `log10(tier)` with a moving window) so each band gets its own visible ramp.
5. **Relative headroom on sinks.** `BCE_TRANSFER_FLOOR_LOG = 10` ("10 decades of headroom") is a *fixed additive* margin on a `double` log10 value.
   At magnitude `10^100` a `double`'s ulp is about `2×10^83`, so `1e100 + 10 == 1e100` exactly — the headroom silently rounds away, `log10subtract`
   reports "no room", and the last slice of every fill is refused forever (the known failure mode, now unavoidable at scale). **From band ~4 every
   sink ceiling must be defined as a multiple of its own magnitude** (e.g. `capacity = bandMassLog × 1.001`), never `bandMassLog + 10`.
   The same rounding means any transfer smaller than ~`1e-13` of a pool no-ops: acceptable at every band (relative error is constant), but stop
   describing it as "a hair of mass" at band 6 and later.
6. **The "effectively unlimited" `1e18` caps become real limits — at band 2, not band 5.** `MANIPULATOR_BCE_CAPACITY_LOG` / `DECOMP_BCE_CAPACITY_LOG` are
   `1.0E18`. That clears band 1 (a tier-`10^12` block's mass log is `9.5×10^11`) but a tier-`10^20` block is `9.5×10^19` — **~100× over the cap** — so both
   constants stop being "unlimited" the moment band 2 exists and would refuse legitimate feeds. They must derive from the band table (§4.1), and band 3's
   instant decompressor needs a ceiling keyed to the **top** band it may ever receive (see §4.16 / D15).
7. **Display: exponent-of-exponent.** Tooltips print `m × 10^e` with a superscript exponent. At the top bands `e` itself is a 100-digit number, so the
   renderer needs a nested form (`10^(9.54 × 10^99)`) or a band-local unit. Same for `DisplayNames.bceMagnitude`.
8. **Craft paths for every machine core — do this now, placeholders allowed.** Today **every multiblock core is creative-only**, so with nine bands the
   ladder has no gate at all. Agreed approach: give all three shipped cores real recipes *before* band 2, and treat the ingredient lists as placeholders
   to retune later (mark them so they are easy to find). Shape to follow: the generator recipes already in the mod — gated by
   `Progression.hasCompressedTier(gate)` on the previous band's top tier, with the top block itself matched by `DataComponentIngredient` on its exact
   tier. Keep placeholder costs *derivable* (previous band's top block + frame/glass/port/interface items, which the player already crafts on the way),
   and watch the known input-matching trap: a recipe that matches a `block_state` component matches it **exactly**, so do not pin a port variant unless
   that is intended. Every new band then adds one row of recipes, not a new system.
9. **FE pricing curve per band.** Band 1's generator doubles per rung from 1,000 FE/t, ending at **512,000 FE/t** — already far above anything else in
   the mod, and a doubling-per-rung rule across nine bands reaches absurdity by band 4. Decide a curve (recommended: keep rung *steps* gentler, ~×1.6,
   and let the **band** multiplier carry the progression; or make the top rungs want N power ports rather than N FE/t through one — which §4.15 pairs with).
10. **Structure QoL — resolved by Patchouli.** The in-world hologram preview (§5) replaces the "missing block highlight / builder kit" idea, and it is what
    makes D2's ever-growing shells playable.
11. **Capability-cache regression check.** The "core placed last → ports cached null → tower dead until relog" bug is fixed (each core's `onLoad()`
    invalidates the shell box). Every new band must keep that call **with the band's own radius** (see §4.12 — the same ±2 cube is hardcoded there too),
    and every in-game test should place the core **last, with cables and pipes already attached**, because that is the ordering that used to fail.
12. **Port→host resolution must stop being a fixed ±2 cube.** ⚠️ *Hard blocker for band 2.* `IMultiblockPortHost.MAX_REGION_RADIUS = 2` is exactly right
    for a 5×5×5 shell, and a 7×7×7 shell puts its face-centre ports **3** cells from the core — so from band 2 on, ports (and the interface's status
    readout) would never find their host and the tower would silently do nothing. Same for the `±2` loops in `invalidatePortCaps` and `findController`.
    Recommended fix: derive candidate core positions *analytically* from the port's own shell role and a `MAX_SHELL_RADIUS` (a face port looks straight
    inwards; an edge/corner port has a couple of diagonal candidates), instead of scanning a `(2r+1)³` cube — which also keeps resolution cheap as
    `r` reaches 9. The band table should own the radius so nothing else has to know.
13. **Rescan cost on big shells.** A formed core re-reads its whole box every 20 ticks; volume goes 125 (`5³`) → 6859 (`19³`), and the generator tries two
    mirrored boxes (~10k block reads per pass at band 8). Fine as a safety net, wrong as the only trigger: have our four shell blocks (frame/glass/port/
    interface) push a dirty flag to their core on placement/removal/state change (they can reuse §4.12's analytic walk), and keep the periodic full scan
    at a slow TTL. Also worth an early-out on the first mismatch, which the current scan already does.
14. **Unloaded cells un-form big towers.** `matchesStructure` bails out if any cell is not loaded — harmless at 5×5×5, but a 19- or 21-wide structure spans
    up to two chunk boundaries, so standing at one edge could drop `formed` mid-run. Recommended: treat an unloaded cell as *unchanged* (keep the last
    known layout, don't reform and don't reset pools), and only fail the structure on cells we can actually read. Needs an in-game decision when band 4
    lands — logged as D11.
15. **Port counts should scale with the band.** One POWER port feeding a `10^50`-tier tower is both silly and a single-cable bottleneck; a 19×19×19 shell
    has 462 face-centre cells to choose from. Put "required POWER ports / BCE-out ports / interfaces" in the band table (e.g. `1, 1, 1` at band 1 rising
    to `4, 2, 2` at band 8) rather than the current `MIN_GEN1_POWER_PORTS`-style constants, and let more ports mean more parallel FE/drain, not more math.
16. **A decompression step must be allowed to drop more than one tier.** With the family ending at band 3 (§2/D7), the machines that remain have to cover
    every tier, and `t → t−1` cannot. Generalise the step: a descent lands on *some lower tier* and releases `massLog(current) − massLog(new)` — the same
    log-space difference the code already computes, just with a bigger jump — then outputs the block at the new tier. Band table owns the rate:
    - band 1 (shipped): 1 tier/tick — fine at its own scale (`10^3` descents in seconds), useless above ~`10^6`.
    - band 2: **geometric descent is the natural answer** — land halfway to the goal, `new = (t + goal) / 2`, which is the exact inverse of the
      compressor's doubling (`out = n.shiftLeft(1)`). Any goal is reached in `log2(t − goal)` steps: from `10^20` in ~67 steps (≈3 s), from `10^100` in
      ~333 steps (≈17 s). Tier-agnostic by construction, so it never needs a successor.
    - band 3: **instant** — one action, straight to the goal. Since it can receive *any* tier, its pool ceiling derives from the top band's mass
      (`~9.5×10^99`), not its own, and the release is bounded by what the network can take per tick (pipe ceilings + §4.5 headroom), so the rest parks in
      the tank until drained. Also needs a one-shot FE price worth deciding (D15).

---

## 5. Patchouli — the band manual and in-world multiblock previews

Chosen answer to "19×19×19 by hand is masonry": the player gets a book entry per machine with a **visualisable** multiblock, so the shell can be previewed
in world (and rotated) before placing 1,946 blocks.

**Facts checked (Oct 2026):** Patchouli ships a maintained **NeoForge 1.21.1** build (`1.21.1-92`/`-93-NEOFORGE`), and from 1.21.1 onward it dropped
MinecraftForge for NeoForge — so our version is the actively published loader. Maven through Modrinth is
`maven.modrinth:nU0bVIaL:h6hKI2ob`; **pin the file ID, not a version number**, because this project already hit the "Modrinth maven serves the wrong
loader's jar for a shared version_number" trap. Patchouli's own repo is preferred if we want transitive metadata. Verify at implementation time whether
the 1.21.1 build needs **Vellum** (its extracted GUI library) declared separately.

**Integration shape:**
- **Soft dependency.** Add it as `compileOnly`/`localRuntime` + an *optional* entry in `neoforge.mods.toml`, so RCC still loads with Patchouli absent (the
  book JSON is inert without it). That keeps the mod dependency-free, which is the whole point of Patchouli.
- **One book, one chapter per band.** `rcc:patchouli_books/rcc_manual/...`, book JSON is resource-pack based (enforced since 1.20): entries under
  `assets/rcc/patchouli_books/rcc_manual/en_us/entries/<band>/…json` plus `book.json` with the landing page and the shell-block glossary.
- **Generate it from the band table.** Patchouli multiblocks are plain JSON: a `pattern` (layer arrays, indexed y top→bottom, then x west→east, then z
  north→south) plus a `mapping` from letters to **blockstate predicates** (`"F": "rcc:compression_frame"`, `"P": "rcc:compression_port[port_type=power]"`,
  `"0"` = the centre cell = the core, space = air, `_` = any block), with `symmetrical: true` for our cubes and `offset` if needed. Since the geometry is
  already a rule (`2k+3`, boundary/face/centre roles), a datagen provider can emit every band's pattern from the same table the scan uses — **the book and
  the scanner cannot drift apart**, which is the failure mode this doc is otherwise full of.
- **Progression wiring.** Entries support an `advancement` field, so a band's pages stay hidden until the player hits that band's milestone — the manual
  doubles as the gate explanation that §4.8's recipes provide.
- **Extra page types we'll want:** `patchouli:crafting` (per-band core recipes), `patchouli:image` (the rung ladder / BCE economy diagrams),
  `patchouli:relations` (compressor ↔ decompressor ↔ generator).
- **Things to test, not assume:** a 19³ hologram is ~6.9k rendered cells — check client FPS with the biggest preview; the generator tower has **two valid
  vertical readings** (core above or below the 1×1 air shaft), so its page should show one and say the other is equally fine; port pages must name the
  right `port_type` variant, and the wrench-cycle (POWER → ITEM_IN → ITEM_OUT) belongs in the text, not the pattern.
- **Fallback if Patchouli ever stalls:** render the preview ourselves in the interface screen with a BER/sculk-shader-style ghost — strictly more work, and
  it loses the book, so treat it as plan B only.

---

## 6. Numerical safety (checked)

- `double` log10 values are finite to ~`1.8×10^308`; a tier of `10^100` gives a mass log of `~9.5×10^99` — five orders of headroom, so **all existing
  log-space math survives to the goal band**. `BigInteger.TEN.pow(100)` is trivial.
- `bceMassLog(BigInteger)` already calls `doubleValue()`, so it handles `10^100` tiers directly.
- Precision is *relative*, never absolute: budget ~`1e-16` relative drift on every buffer, and see §4.5 for the headroom consequence.
- Watch `int` FE fields: `energyCapacity = perTick × 20` overflows at `perTick > 107,374,182`. With a doubling ladder that breaks at band ~2, another
  reason for §4.9.

---

## 7. UI / content organisation

- **Patchouli is the guide, JEI is the numbers.** The book (§5) carries structure previews, the shell vocabulary and the "what unlocks what" story; JEI
  keeps doing per-tier chain lookups and rates. Don't duplicate prose in both — link book entries from JEI pages if we want cross-references.
- **Creative tabs:** the two-tab split (resources | machines & logistics) is already crowded at band 1's ten generator rungs. Plan for **sub-tabs or a
  band sub-grouping** before band 3, otherwise the machines tab becomes unusable (~200 entries by band 8).
- **JEI:** one info page per band per machine family, plus the compression chain category as it stands.
- **Advancements:** keep the existing linear chain but add **one milestone per band** (obtain the band's top block) rather than per rung; band 1's ten
  rung advancements would become ~90 at band 8.
- **Naming:** bands read as Roman numerals (`Generation Manipulator I…IX`), which `DisplayNames.roman` already does; block levels keep the Latin
  multiplicative series (`tupleWord`) and the power-of-ten words for the huge tiers.

---

## 8. Progress tracker

### ✅ Tier 0 band — complete
Manual 3×3 chain to tier 1000 · Auto-Compressor (single block, tier→1000, BCE fuel sink) · Auto Decompressor (single block, BCE buffer) ·
cobblestone generator ladder (`BASE` 40/sec → `9^500`/sec, item + BCE rungs, advancement-gated chain) · BCE pipe tier I · BCE Storage I.

### ✅ Tier 1 band — complete (just shipped)
Compression Manipulator 5×5×5 (fuel pool, seed/output ports, GUI, lightning BER) · Decompression Manipulator 3x3x3 (1 tier/tick descent, goal tier input) ·
Generation Manipulator 3×3×4 (10 rungs `9^1000/s` → `9^(5×10^11)/s`, FE-priced, single BCE-out drain, required interface panel + chat readout) ·
shared shell blocks (frame / glass / port / interface) · wrench-cycled ports and pipe ends, any-mod wrench via `c:tools/wrench` ·
BCE pipe tier II · BCE Storage II · shared `IMultiblockPortHost` port contract · network bus with per-transfer absolute ceilings ·
capability-cache invalidation on core load.

**Deferred / optional (offered, never requested):** advancements for the multiblock cores · **crafting recipes for any multiblock core (all creative-only
— now scheduled as §4.8 / Phase A, no longer optional)** · JEI info page for the Generation Manipulator · FE ladder retune (top rung = 512,000 FE/t).

### ⏳ Phase A — gate + guidance infrastructure — not started
Runs **before** band 2 content: core recipes with placeholder costs (§4.8), the port→host radius generalisation (§4.12 — band 2 is dead without it),
the band table + `band` property (§4.1–4.2), exponent migration (§4.3), tint/display/headroom fixes (§4.4–4.7), **and the Patchouli book written against
the two structures that already exist** (5×5×5 and 3×3×4) so the integration is verified in game before any band bets a 19×19×19 shell on it.

### ⏳ Tier 2 band — not started
Target: `10^12 → 10^20`, 7×7×7 compressor, 5×5×5 decompressor (**rate to be decided — must not stay 1 tier/tick, see §4.16 / D14**), 5×5×6 generator
tower, pipe III, storage III, all read from the band table, each with a book entry + visualisable pattern.

### · Tiers 3–8 — planned, see §2
`10^28, 10^38, 10^50, 10^64, 10^80, 10^100`. Bands 2–3 finish the decompressor family (band 3 = instant); **bands 4–8 ship no decompressor at all**.

### Endgame (band 8) — unresolved design question
What does a player *do* with a tier-`10^100` block? Today the only sink for mass is "burn it as fuel" — and the band-3 **instant decompressor** is already
a partial answer: one action turns the googol block into a BCE flood limited only by how fast your pipes and tanks can take it. That is a real end-use,
but not a *goal*. A final band still wants a purpose — an artifact/achievement block, a world effect, a "spend the googol" consumer, or a credits-style
milestone. Decide before band 6 so the ladder's shape can support it.

---

## 9. Decision log

### ✅ Resolved

| # | Question | Decision |
| --- | --- | --- |
| D1 | Goal in **tier** units or BCE-multiplier units? | **Tier `10^100`.** A googol of BCE is only tier ~105, already beaten in band 0 — it cannot be the goal. |
| D2 | Do the multiblocks keep growing, or does the footprint cap? | **They keep growing** (compressor 5→19, tower 3×3×4→17×17×18). Big-build playability comes from **Patchouli's in-world multiblock preview** (§5), not from capping sizes. |
| D2a | Recipes now or later? | **Now, placeholders allowed** (§4.8, Phase A). All three shipped cores get craft paths before band 2. |
| D2b | Structure QoL approach | Patchouli hologram preview beats a home-made "missing block" highlight or builder kit. |
| D7 | Does every band get a decompressor? | **No — the family ends at band 3.** All decompressors already accept any tier, so only speed differs; band 2 gets a fast rate (§4.16) and band 3 an **instant one-action** descent to the goal. Bands 4–8 ship none. |

### · Still open

| # | Question | Recommendation |
| --- | --- | --- |
| D3 | New core block per band, or one core with a `band` property? | `band` property (matches the rung pattern, cheap registrations) |
| D4 | Rungs per band (10 today) and the creative-tab blow-up | ≤12 rungs/band, band-grouped tabs |
| D5 | FE cost curve across nine bands | Gentler per-rung steps, band-level multiplier, more ports at the top |
| D6 | Advancement granularity | One milestone per band (also unlocks the book chapter) |
| D8 | Art budget per band (~6 textures) | Recolour ramps on shared models, as the pipes already do |
| D9 | Purpose of the final block (§8 endgame) | Needs a real answer before band 6 |
| D10 | 9 bands or 10? | 9; split band 8 if it feels short |
| D11 | Big towers span chunks: should an unloaded shell cell keep the machine formed? | Yes — assume unchanged, don't reset pools (§4.14) |
| D12 | Patchouli hard or optional dependency? | Optional (`compileOnly` + soft mod entry), so RCC stays dependency-free |
| D13 | Placeholder recipe costs | Previous band's top block + shell items; marked for later retuning |
| D14 | Band 2 decompressor rate — it cannot stay 1 tier/tick (that is a ~1,600-year descent at its own top tier) | Geometric halving `new = (t + goal)/2` — inverse of the compressor's doubling, ~3 s from `10^20` |
| D15 | Band 3 instant decompressor: does it accept *any* tier, and what does one action cost? | Yes (pool ceiling from the top band, drain capped by pipe ceilings); one-shot FE price scaled to the tiers dropped |
| D16 | Is the instant decompressor a band-3 *structure* or an upgrade of band 2's? | Separate 7×7×7 structure — it is the family's finale and should read as one |

---

## 10. Suggested build order

1. **Phase A — gate + guidance infrastructure** (§4.8 recipes, §4.12 resolution generalisation, §4.1–4.7 band table + numeric/display/tint fixes, then the
   Patchouli book for bands 0–1). Verified by playing band 1 unchanged, crafting its cores, and visualising both existing shells in world.
2. **Phase B — band 2 vertical slice**: compressor 7×7×7, decompressor 5×5×5, tower 5×5×6, pipe III, storage III, recipes, advancement, lang, art,
   book entry with the visualisable pattern. This band is the first one *entirely* driven by the band table.
3. **Phase C — bands 3–8**: each is one content pass against the slice template (a new row + art). Stop after each band for in-game verification.
4. **Phase D — endgame**: final milestone, purpose of the top block, JEI/book tour, config file for band tuning, placeholder recipe retune.

---

## 11. Verification convention (unchanged, per band)

`gradlew compileJava` and `gradlew runData` headless, generated files read directly (tags under `src/generated/resources/data/minecraft/tags/block/...`),
never `runClient`/`runServer`. The user tests in game — including the place-the-core-last case — and approves before the next band starts.

*Last updated: bands 0–1 complete; goal fixed as tier `10^100`; shells keep growing with Patchouli previews; core recipes scheduled in Phase A;
decompressor family ends at band 3 (instant). Open: D3–D6, D8–D16.*
