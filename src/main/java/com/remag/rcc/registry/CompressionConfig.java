package com.remag.rcc.registry;

import java.math.BigInteger;

/**
 * The single knob for the whole mod: how the compression tiers are bounded, the band-indexed tint ramp that maps a
 * tier to a colour, and the shared math for the big-tier machine's base-cobblestone-equivalent mass.
 *
 * <p>Tiers are {@link BigInteger} and represent an <em>emergent</em> amount of cobblestone: a tier-{@code t}
 * block is equivalent to {@code 9^(t-1)} base cobblestone, but we never materialise that count. Anything that
 * needs a comparable magnitude works in base-10 log space (a {@code double}), which stays finite and monotonic
 * even toward a googolplex-tier block.
 */
public final class CompressionConfig {
    // --- Tier bounds ---------------------------------------------------------------------------
    // These are <em>aliases onto the band table</em> ({@link CompressionBand}), not the other way round: one row per
    // band is the single source of truth, and the shipped names stay so the machines read the same numbers they always
    // did. Band 0 tops out at {@code 10^3 = 1000} and band 1 at {@code 10^15}, one rung ladder per band.
    /** Highest tier the vanilla 3x3 compress chain can produce; also the creative-tab listing ceiling. */
    public static final BigInteger MANUAL_MAX_LEVEL = CompressionBand.BAND_0.maxOutputTier();
    /** Lowest seed tier the Compression Manipulator will accept. */
    public static final BigInteger MANIPULATOR_MIN_IN = CompressionBand.BAND_1.minInputTier();
    /** Highest output tier this machine will produce (a future machine continues the ladder). */
    public static final BigInteger MANIPULATOR_MAX_OUT = CompressionBand.BAND_1.maxOutputTier();

    // --- Visual curve --------------------------------------------------------------------------
    // The tint is the one place a tier becomes something you can <em>see</em>, so the ramp is indexed by band rather
    // than normalised against a single ceiling. The old curve (MASTER_PLAN D21) was
    // {@code brightness = 1 - log10(tier) / 12}: grayscale only, and it clamped to pure black at band 1's ceiling, so
    // every band-2..8 block was indistinguishable from a band-1 one. It is replaced by one colour <em>segment per
    // band</em> - band 0 darkens plain cobblestone to black, band 1 climbs back out of black to white, band 2 pushes
    // white into red, and the rest walks the rainbow - so each machine tier visibly recolors the block, and the two
    // grayscale segments still read exactly like the old ramp for the tiers that exist today.
    /**
     * The tint that reproduces the <em>original</em>, un-brightened cobblestone art on a tier-1 block: {@code 177/255}
     * per channel, in {@code [0,1]}.
     *
     * <p>Needed because a tint is a <b>multiply</b> and can never make a pixel brighter than its texture already is.
     * The compressed cobblestone sprite ({@code assets/rcc/textures/block/compressed_cobblestone.png}) is a deliberate
     * brightened derivative of the mod's own cobblestone art - every channel scaled by {@code 255/177}, pushing its
     * brightest pixel to full white so that a {@code (1,1,1)} tint actually reads as white instead of the muddy grey
     * vanilla-derived art gives. A pure scale is exactly reversible, so multiplying back by {@code 177/255} - this
     * constant - lands band 0's start on the original pixels, unchanged.
     *
     * <p><b>Repainting the sprite means updating this number</b>: it is the repainted texture's own
     * {@code max_channel / 255} and nothing else. Keep the brightening a plain scale (no gamma, no lift) or band 0
     * stops matching the art it is meant to reproduce.
     */
    public static final float TINT_BASE_GREY = 177f / 255f;
    /**
     * The ramp's <em>anchors</em>, one per entry of {@link #TINT_STOPS}, as {@code 0xFFRRGGBB}: index 0 is the
     * plain-cobblestone colour of a tier-1 block, and index {@code k+1} is the colour a block reaches at the top of
     * band {@code k}. Ordered by progression: cobblestone grey, black, white, then red, orange, yellow, green, cyan,
     * blue, violet - one hue per band, the rainbow the plan asks for (D21 resolved).
     */
    public static final int[] TINT_ANCHOR_ARGB = {
            argbGrey(TINT_BASE_GREY),
            0xFF_000000, // top of band 0 (tier 1000): the whole manual climb darkens
            0xFF_FFFFFF, // top of band 1: the multiblock band climbs back out of black to white
            0xFF_FF0000, // band 2: white -> red
            0xFF_FF8000, // band 3: red -> orange
            0xFF_FFFF00, // band 4: orange -> yellow
            0xFF_00FF00, // band 5: yellow -> green
            0xFF_00FFFF, // band 6: green -> cyan
            0xFF_0000FF, // band 7: cyan -> blue
            0xFF_8000FF  // band 8: blue -> violet, the mod's stated googol-tier look
    };
    /**
     * The {@code log10(tier)} at each {@link #TINT_ANCHOR_ARGB} anchor, derived from the band table so a new band row
     * adds a ramp segment on its own: {@code 0} (tier 1), then every {@link CompressionBand#maxOutputTier()}.
     * <p>Computed through {@link #log10(java.math.BigInteger)}, a method, so it is safe from a static initialiser -
     * {@link CompressionBand}'s constructors never read back into this class (see its own javadoc on the order rule).
     */
    public static final double[] TINT_STOPS = buildTintStops();

    /** {@link #TINT_STOPS} paired with {@link #TINT_ANCHOR_ARGB}: one entry per band ceiling, led by tier 1. */
    private static double[] buildTintStops() {
        CompressionBand[] bands = CompressionBand.values();
        double[] stops = new double[bands.length + 1];
        stops[0] = 0.0D; // log10(1): the tier-1 block, still looking like plain cobblestone
        for (int i = 0; i < bands.length; i++) {
            stops[i + 1] = log10(bands[i].maxOutputTier());
        }
        return stops;
    }

    /** A grayscale anchor from a single {@code [0,1]} channel value, in the {@link #TINT_ANCHOR_ARGB} format. */
    private static int argbGrey(float channel) {
        int v = Math.clamp(Math.round(255f * channel), 0, 255);
        return (0xFF << 24) | (v << 16) | (v << 8) | v;
    }

    /** log10(8): the extra base-cobblestone mass a single +1 tier step needs beyond the seed it upgrades. */
    public static final double LOG8 = Math.log10(8d);

    // --- Machine / mass math -------------------------------------------------------------------
    /** log10(9): the base-cobblestone multiplier each compression tier adds. */
    public static final double LOG9 = Math.log10(9d);

    // The two headroom margins sit here, ahead of every band-derived ceiling below, because those fields call
    // {@link #withTransferHeadroom} <em>from their own initialisers</em>: static fields run top to bottom, so a
    // margin declared later would still be {@code 0.0} when an earlier ceiling asks for it and the headroom would
    // silently vanish (a sink with a ceiling exactly equal to its biggest push can never express "one slice of room
    // left", which is the failure this margin exists to prevent).
    /**
     * The smallest transfer a log10 buffer can physically express. Doubles carry ~15 significant digits, so at
     * magnitude ~954 anything more than ~13 orders smaller than the pool no-ops on add/subtract - moving such a
     * slice would silently create a hair of mass. <strong>No longer a transfer gate:</strong> {@code BceNetwork}
     * runs every leg unconditionally and accepts that bounded, invisible sliver (the end goal is so many orders away
     * that the duplication is nothing). This constant now only sizes a store's headroom above its nominal capacity
     * (see {@link #AUTO_BCE_CAPACITY_LOG}), so a sink always has room to express the last slice of a transfer.
     */
    public static final double BCE_TRANSFER_FLOOR_LOG = 10.0D;
    /**
     * The <em>relative</em> margin a sink keeps above its own biggest push. Needed because a {@code double} log10
     * value this large cannot carry an additive ten: at magnitude {@code 1e100} one ulp is about {@code 2e83}, so
     * {@code 1e100 + 10 == 1e100} exactly and a fixed headroom silently rounds out of existence - {@link #log10subtract}
     * then reports "no room" and the last slice of every fill is refused forever. Taking the <em>larger</em> of the
     * additive and relative margins in {@link #withTransferHeadroom} keeps today's small-band behaviour byte-identical
     * while staying expressible at the goal band.
     */
    public static final double SINK_HEADROOM_RELATIVE = 1.0E-3D;
    /**
     * The old hand-picked "effectively unlimited" sink ceiling, kept as a <em>floor</em>. {@link
     * CompressionBand#sinkCeilingLog()} takes the larger of this figure and its own band's biggest push, so no
     * shipped band's pool gets tighter than it is today (band 1's whole ceiling block is only {@code 9.5*10^14} log
     * mass, far under this) while every band from 2 upward is governed by its own magnitude instead of a constant
     * that would quietly start refusing legitimate feeds.
     */
    public static final double UNLIMITED_SINK_FLOOR_LOG = 1.0E18D;

    // --- Manipulator multiblock ---------------------------------------------------------------
    public static final int REGION_RADIUS = CompressionBand.BAND_1.compressorRadius(); // 5x5x5 cube centred on the core
    public static final int ENERGY_CAPACITY = 1_000_000;
    public static final int ENERGY_PER_CRAFT = 250_000;
    /**
     * Craft time for the <b>first</b> multiblock tier (band 1), in ticks: the seed every higher band scales from via
     * {@link #craftTicks(CompressionBand)}. Change <em>this one figure</em> to speed up or slow down the whole ladder -
     * the higher bands are pure multiplication, not hand-authored per-tier literals. {@code 240 = 12s} is the current
     * test value (the progress bar and the FE drain rate both derive from it).
     */
    public static final int TICKS_PER_CRAFT = 240;
    /**
     * How much slower each band above tier 1 crafts, as a multiplier on the tier-1 {@link #TICKS_PER_CRAFT}: band 1 is
     * the base, band 2 is {@code 1.5x}, band 3 {@code 1.5^2}, and so on, so a higher tier costs visibly more time per
     * craft without a per-band table. Applied in {@link #craftTicks(CompressionBand)}.
     */
    public static final double CRAFT_SLOWDOWN_PER_BAND = 1.5D;
    /**
     * POWER ports the Manipulator's shell needs at minimum, from its band row. Band-scaled port counts are how a
     * later band raises its FE budget without one enormous number (see {@link CompressionBand#minPowerPorts()} and
     * MASTER_PLAN §4.15); bands 0-1 both ask for one.
     */
    public static final int MIN_POWER_PORTS = CompressionBand.BAND_1.minPowerPorts();
    public static final int MIN_ITEM_IN_PORTS = 1;
    public static final int MIN_ITEM_OUT_PORTS = 1;
    /** The structure needs exactly one Interface block (the only GUI entry point); more makes no sense. */
    public static final int MIN_INTERFACES = 1;
    public static final int MAX_INTERFACES = 1;
    /**
     * Ceiling of the Manipulator's BCE-as-fuel intake (the ITEM_IN port's sink view), in log10 BCE - derived from its
     * band instead of being a hand-picked literal. {@link CompressionBand#sinkCeilingLog()} grows this with the band,
     * floored at {@link #UNLIMITED_SINK_FLOOR_LOG}, which lands on exactly the old "effectively unlimited"
     * {@code 1e18} for the shipped band 1 (so today's behaviour is untouched) and lets a band-2 pool scale to its own
     * biggest push instead of a constant that would start <em>refusing legitimate feeds</em> - a tier-{@code 10^27}
     * block is worth {@code 9.5*10^26} log10 mass, well past that old figure. See {@link #withTransferHeadroom} for
     * the margin that keeps the last slice of a fill expressible.
     */
    public static final double MANIPULATOR_BCE_CAPACITY_LOG = CompressionBand.BAND_1.sinkCeilingLog();

    // --- Auto-Compressor (single-block tier 1..1000 automation; see machine.CompressionAutoBlockEntity) ---------
    /** The seed tier the Auto-Compressor climbs to; on reaching it the result is moved to the output slot. */
    public static final BigInteger AUTO_TARGET_LEVEL = MANUAL_MAX_LEVEL; // 1000
    /** One +1 tier step completes after this many ticks (tune for climb speed). */
    public static final int AUTO_TICKS_PER_STEP = 5;
    /** FE buffer of the Auto-Compressor. */
    public static final int AUTO_ENERGY_CAPACITY = 100_000;
    /** FE consumed by a single +1 tier step. */
    public static final int AUTO_ENERGY_PER_STEP = 1_000;

    // --- Auto Decompressor (single-block reverse of the ladder; see machine.CompressionAutoDecompressorBlockEntity) --
    /**
     * Hard ceiling of the Auto Decompressor's BCE buffer, in log10 BCE. Keyed to {@link CompressionBand#TOP}, not to
     * its own band, because <em>every</em> decompressor is tier-agnostic: it will accept any block the mod can produce
     * and walk it down (that is what lets the decompressor family end at band 3), and a descent from a googol-tier
     * seed releases {@code ~9.5*10^99} log10 mass. A ceiling that any reachable input could exceed would stall a legal
     * descent, so this stays effectively unlimited - it is the drain (pipes, tanks, fuel) that is the real constraint.
     */
    public static final double DECOMP_BCE_CAPACITY_LOG = CompressionBand.TOP.sinkCeilingLog();
    /**
     * The quantum the GUI's BCE bar is measured against: one tier-{@link #AUTO_TARGET_LEVEL} block's worth of BCE
     * ({@code 9^1000}). Kept separate from {@link #DECOMP_BCE_CAPACITY_LOG} on purpose - a bar whose denominator were
     * 10^1e18 would never leave 0%, so the gauge shows "fill relative to a tier-1000 block" (it saturates once the
     * tank holds more than that) while the stall gate above stays open.
     */
    public static final double DECOMP_BCE_DISPLAY_LOG = bceMassLog(AUTO_TARGET_LEVEL);

    // --- Decompression Manipulator (the 3x3x3 multiblock reverse of the Compression Manipulator) ---------------
    /**
     * Half-width of the tier-1 decompressor multiblock: a radius of {@code 1} is the smallest cube with a distinct
     * centre (the controller) and a one-block shell around it - the {@code 3x3x3} structure with a hollow centre. Its
     * shell shares the Manipulator's frame / glass / port / interface blocks but allows ports on the edge and corner
     * cells too, because the {@code 6} face-centre cells alone cannot fit one interface plus one of every port.
     */
    public static final int DECOMP_REGION_RADIUS = CompressionBand.BAND_1.decompressorRadius();
    /**
     * Ticks one tier step of the Decompression Manipulator takes. At {@code 1} the multiblock releases one tier per
     * game tick - five times the single-block Auto Decompressor's pace ({@link #AUTO_TICKS_PER_STEP}), which is the
     * reward for building the structure - while still being a visible descent rather than an instant collapse.
     */
    public static final int DECOMP_MULTIBLOCK_TICKS_PER_STEP = 1;
    /**
     * FE burned per multiblock tier step. Priced the same as the single block (the structure buys speed, not
     * efficiency), but kept as its own constant so the per-tick FE draw can be tuned against what the POWER ports
     * can realistically supply - at one step per tick this machine consumes that much <em>every</em> tick.
     */
    public static final int DECOMP_MULTIBLOCK_ENERGY_PER_STEP = AUTO_ENERGY_PER_STEP;

    // --- Tier-1 generator multiblock (the 3x3x4 BCE fountain; see machine.GeneratorCoreBlockEntity) -------------
    /**
     * FE/tick burned by the <em>first</em> rung of the tier-1 generator multiblock while minting; every rung above
     * doubles it (see {@link com.remag.rcc.machine.GeneratorCoreTier#energyPerTick()}), so the top rung demands
     * {@code 1000 << 9 = 512,000} FE/t. The escalating bill is what makes the ladder a power-infrastructure gate
     * the way the tier-0 buffer ceilings were a logistics gate.
     */
    public static final int GEN1_BASE_ENERGY_PER_TICK = (int) CompressionBand.BAND_1.baseEnergyPerTick();
    /**
     * Half-width of the tier-1 generator's horizontal shell ({@code 3x3} columns) - same ring rule as the 3x3x3
     * decompressor - while the column runs four layers tall ({@link #GEN1_LAYERS_BELOW} under and
     * {@link #GEN1_LAYERS_ABOVE} over the core).
     */
    public static final int GEN1_REGION_RADIUS = CompressionBand.BAND_1.generatorHalfWidth();
    /** Layers under the core when it seals the <em>lower</em> interior slot of the four-tall column. */
    public static final int GEN1_LAYERS_BELOW = CompressionBand.BAND_1.generatorLayersBelow();
    /**
     * Layers above the core when it seals the <em>lower</em> interior slot (mirrored when it seals the upper one).
     * The two interior slots are core + 1x1 air shaft; the scan tries both readings of the column.
     */
    public static final int GEN1_LAYERS_ABOVE = CompressionBand.BAND_1.generatorLayersAbove();
    /** The generator multiblock needs at least one POWER port... */
    public static final int MIN_GEN1_POWER_PORTS = CompressionBand.BAND_1.minPowerPorts();
    /** ...and <em>exactly</em> one BCE-out (ITEM_OUT) port, so the mint rate never splits across two drains. */
    public static final int EXACT_GEN1_BCE_OUT_PORTS = 1;

    // --- BCE logistics: pipes + storage (see energy.BceNetwork, machine.BcePipeBlock) --------------------------
    /**
     * Throughput of each BCE pipe tier as an <em>absolute per-transfer ceiling</em>, in log10 BCE, indexed by tier
     * ordinal. A pipe moves at most this much and at most whatever its source holds, whichever is smaller - so the
     * three tiers map onto the three shipped machine bands: tier I tops out at one tier-{@link #MANUAL_MAX_LEVEL}
     * block's BCE (band 0, the Auto-Compressor band), tier II at one band-1 block's BCE, tier III at one band-2 block's
     * BCE. Each value is {@code CompressionBand.BAND_k.pipeCeilingLog()}, so adding a band tier is one array entry.
     *
     * <p>An absolute ceiling replaced the old fraction-of-source rate because the bands are now fixed: tying a tier to
     * its machine's biggest block makes the pipe's job legible ("drain one tier-1000 block per transfer") and, unlike
     * a percentage, lets a huge source still feed a much smaller sink instead of the two orders of magnitude refusing
     * to meet. Values derive straight from the band constants so they stay tunable in one place.
     */
    public static final double[] BCE_PIPE_MAX_TRANSFER_LOG =
            {CompressionBand.BAND_0.pipeCeilingLog(), CompressionBand.BAND_1.pipeCeilingLog(),
                    CompressionBand.BAND_2.pipeCeilingLog()};
    /** Capacity of the single-block BCE storage tank, in log10 BCE: one tier-{@code 1000} block's full yield. */
    public static final double BCE_STORAGE_CAPACITY_LOG = CompressionBand.BAND_0.storageCapacityLog();
    /**
     * Capacity of the <em>tier-two</em> tank, in log10 BCE: one band-1 block's full yield - the biggest thing the
     * band-1 multiblock (Compression / Decompression Manipulator) can make or release. It is deliberately
     * {@code BCE_PIPE_MAX_TRANSFER_LOG[1]} on the transfer side: one tier-II pipe push fits the whole tank, so the
     * multiblock's mass has a vault that neither the tier-I pipe band nor the original tank (sized to the tier-1000
     * Auto band) can strain.
     */
    public static final double BCE_STORAGE_2_CAPACITY_LOG = CompressionBand.BAND_1.storageCapacityLog();
    /**
     * Capacity of the <em>tier-three</em> tank, in log10 BCE: one band-2 block's full yield - the biggest thing the
     * band-2 multiblock can make or release. Same {@code BCE_PIPE_MAX_TRANSFER_LOG[2]}-mirrors-capacity rule as the
     * tier-two tank: one tier-III pipe push fits the whole vault, so the band-2 machines finally have a pipe and a
     * tank that can carry their mass (before this tier existed they had neither).
     */
    public static final double BCE_STORAGE_3_CAPACITY_LOG = CompressionBand.BAND_2.storageCapacityLog();
    /**
     * How many ticks a pipe network may reuse its cached store endpoints before rescanning. Placement and removal
     * invalidate eagerly via {@code neighborChanged}; this is the safety net for stores that appear/disappear
     * without a block update reaching the pipe (chunk load, another mod's machine).
     */
    public static final int BCE_NETWORK_ENDPOINT_TTL_TICKS = 100;
    // (Removed: BCE_PIPE_BRIGHTNESS / BCE_PIPE_EXTRACT_TINT / BCE_PIPE_INSERT_TINT. The pipes used to be four tiers and
    // two directions of the same tinted cube, so the ladder lived in these constants; they are art now - the tier ramp
    // is baked into textures/block/bce_pipe_<tier>.png and the direction into the shape of the arm caps. The ramp the
    // textures follow is the same one the old tint used: brightness 1.0 / 0.72 / 0.5 / 0.32 for tiers 1..4, over an
    // amber (0xFFB46A) extract hue and a blue (0x6AA8FF) insert hue.)
    /**
     * Ceiling of the Auto-Compressor's BCE fuel pool, in log10 BCE: one full storage tank's yield plus the headroom
     * margin above. The margin is not decoration - a sink whose ceiling equals its biggest possible input leaves nothing for
     * {@link #log10subtract} to express as "room", so the last slice of every transfer would be refused forever; ten
     * decades is exactly {@link #BCE_TRANSFER_FLOOR_LOG}, the smallest difference this space can talk about at this
     * band. Item
     * fuel is still uncapped against this ({@code acceptFuel} just accumulates), so the number only gates what a bus
     * is allowed to push in.
     */
    public static final double AUTO_BCE_CAPACITY_LOG = withTransferHeadroom(BCE_STORAGE_CAPACITY_LOG);

    /**
     * A sink ceiling that stays representable at any magnitude: the larger of {@code value + ten decades} (the shipped
     * convention, and what governs at the small bands) and {@code value * 1.001} (the only margin a {@code double}
     * can still express once the value runs to four- and five-digit exponents). See {@link #SINK_HEADROOM_RELATIVE}.
     * Both margins are compile-time constants, so this is safe to call from another static initialiser.
     */
    public static double withTransferHeadroom(double value) {
        if (value == Double.NEGATIVE_INFINITY) {
            return Double.NEGATIVE_INFINITY;
        }
        return Math.max(value + BCE_TRANSFER_FLOOR_LOG, value * (1.0D + SINK_HEADROOM_RELATIVE));
    }

    // --- Cobblestone generators (the progression gate; see machine.CobblestoneGeneratorBlockEntity) ------------
    /**
     * Rate of the very first, item-output generator, in whole vanilla cobblestone per second. This is the entry point
     * of the whole ladder - a hand-held 40/sec drip that a hopper can physically carry away.
     */
    public static final int GEN_BASE_COBBLES_PER_SEC = 40;
    /**
     * The steepest generator milestone, as a compression tier {@code n}: it runs at {@code 9^n} BCE per second, i.e.
     * the BCE of one tier-{@code n} block every tick-equivalent. {@code 500} is the tier whose block ({@code 9^500})
     * is half of the top tier-1000 block's BCE on the log scale, so this caps the ladder at "half the max BCE".
     */
    public static final int GEN_MAX_TIER_EXPONENT = 500;
    /**
     * The BCE milestone compression tiers this ladder walks, ascending; a generator at tier-exponent {@code n} produces
     * {@code 9^n} BCE per second. This is the <em>reference curve</em> - the rungs themselves are declared as
     * {@link com.remag.rcc.machine.GeneratorTier} enum constants (a block-state property needs a fixed set), so adding
     * or removing a generator means editing that enum; keep it consistent with these milestones. These are all
     * {@code n >= 10}, i.e. every one of them is a BCE rung; the item rungs that fill the gap below the transition
     * ({@code BASE} plus {@code 9^n} for {@code n <= 8}, since {@code 9^9} overflows an int) live only in the enum.
     */
    public static final int[] GEN_MILESTONE_EXPONENTS = {10, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500};
    /**
     * The {@link Integer#MAX_VALUE} ceiling: a generator whose per-second output exceeds it cannot express the flow as
     * cobblestone <em>items</em> (a count would not fit an int), so it switches to raw BCE output. The item/BCE mode of
     * a generator is decided against exactly this number.
     */
    public static final double GEN_ITEM_OUTPUT_LIMIT = 2147483647.0D; // Integer.MAX_VALUE, as a log-comparable double
    /**
     * The compression tier of cobblestone that gates the very first ({@link com.remag.rcc.machine.GeneratorTier#BASE})
     * generator - the root of the whole progression line (the "Only the Beginning" advancement). Every other rung is
     * gated by the tier whose block equals its own per-second BCE (a rung at exponent {@code n} needs tier {@code n}),
     * but {@link com.remag.rcc.machine.GeneratorTier#BASE}'s fixed {@link #GEN_BASE_COBBLES_PER_SEC}-per-second drip
     * matches no tier exactly, so its gate is chosen by hand. Kept here so the ladder's entry cost is one tunable
     * number alongside the rest of the gate.
     */
    public static final int GEN_BASE_GATE_TIER = 5;
    /** log10 of this generator's per-second rate: {@code bceMassLog(n)} for a {@code 9^n/sec} milestone, {@code log10(40)} for the base. */
    public static double generatorRateLog10PerSec(int tierExponent) {
        return tierExponent < 0 ? Math.log10(GEN_BASE_COBBLES_PER_SEC) : bceMassLog(BigInteger.valueOf(tierExponent));
    }
    /**
     * Ticks between <em>minting</em> actions of a generator array - one per second. The generator used to add
     * {@code rateLog - log10(20)} to its buffer every tick, which is a quantity that stops existing at the magnitudes
     * the later bands run at: the ulp of a band-2 rate figure is already {@code 8192}, so subtracting {@code 1.301}
     * from it returns the same {@code double} and a tower silently deposited its whole <em>per-second</em> rate
     * <em>every tick</em> (20x its advertised output), while {@link #log10add} could never grow the buffer further.
     * Minting one whole lump once per period makes the delivered amount exactly the advertised amount at every band,
     * and needs no accumulation to be correct.
     */
    public static final int GEN_MINT_PERIOD_TICKS = 20;
    /**
     * Buffer ceiling of a generator array, in log10 BCE, given the lump size it mints: the lump itself plus the
     * headroom {@link #withTransferHeadroom} keeps expressible. Replaces the old {@code rateLog - log10(2)} ceiling,
     * which was sized for per-tick slices and would now refuse the very lump the array exists to produce.
     */
    public static double generatorLumpCeilingLog(double lumpLog) {
        return withTransferHeadroom(lumpLog);
    }

    // --- Seed cube render (the spinning cube drawn at the core; see client.CompressionCoreRenderer) ---------
    /**
     * <em>Band-1 base</em> side length, in blocks, of the seed cube (scale the higher bands grow from via
     * {@link #seedCubeScale(CompressionBand)}). The core is a unit cube whose corners reach {@code sqrt(3)/2 ~ 0.87};
     * a concentric cube only hides it from <em>every</em> rotation once its inradius (scale/2) reaches that, i.e.
     * {@code SEED_CUBE_SCALE >= sqrt(3)}. The base stays well under 3 so band 1's tumbling corners remain inside its
     * 3x3x3 air interior; later bands have a chamber {@code 2} blocks wider per band, which their larger scale fills.
     */
    public static final float SEED_CUBE_SCALE = 1.2F;
    /**
     * Global multiplier on the seed cube's tumble rates. {@code 0} = frozen, {@code 1} = the per-machine baseline,
     * higher = faster spin. The idle cube never spins regardless of this value.
     */
    public static final float SEED_SPIN_SPEED = 4F;

    // --- Crafting lightning & completion pulse (drawn as BER quad ribbons/flash; see client.CompressionCoreRenderer) ---
    /** Master switch for the in-chamber zap storm and the end-of-craft pulse. */
    public static final boolean LIGHTNING_ENABLED = true;
    /** Ticks between zap volleys while a craft runs (smaller = a busier storm). */
    public static final int ZAP_INTERVAL_TICKS = 8;
    /** Bolts fired per volley. */
    public static final int BOLTS_PER_ZAP = 8;
    /** How many straight sub-segments each bolt is broken into (the zig-zag); {@code 1} = a perfectly straight line. */
    public static final int BOLT_STEPS = 6;
    /**
     * Max sideways offset, in blocks, applied to a bolt's interior points to make it jagged. Tapered to zero at both
     * endpoints, so the bolt always starts on the seed cube and lands exactly on the chosen wall point. Raise for wilder
     * bolts, set {@code 0} for straight ones.
     */
    public static final float BOLT_JAG = 0.22F;
    /**
     * Full thickness, in blocks, of each bolt: every segment is drawn as a camera-facing quad ribbon this wide (not a
     * fixed 1px line, which was too thin to see). Raise for fatter, brighter bolts; {@code 0.12F} reads clearly at a
     * normal viewing distance.
     */
    public static final float BOLT_WIDTH = 0.02F;
    /** How long (in ticks) each bolt stays on screen, fading out over its life. Keep short so volleys read as flashes. */
    public static final int BOLT_TTL = 20;
    /** Number of radial spokes in the completion flash (higher = a denser, more ball-like burst). */
    public static final int PULSE_BURST = 40;
    /**
     * Radius, in blocks, at which the flash starts. Set above the seed cube's corner reach ({@code SEED_CUBE_SCALE} *
     * sqrt(3)/2 ~ 1.04) so the flash is visible around the cube even when another seed is still queued and the cube is
     * present.
     */
    public static final float PULSE_START_RADIUS = 1F;
    /** Radius, in blocks, the flash expands to before it fades (kept just inside the inner glass shell at 1.45). */
    public static final float PULSE_END_RADIUS = 1.4F;
    /** Radial length, in blocks, of each flash spoke. */
    public static final float PULSE_LEN = 1F;
    /** Full thickness, in blocks, of each flash spoke (its own width, independent of the thin bolts). */
    public static final float PULSE_WIDTH = 0.04F;
    /** How long (in ticks) the completion flash expands and fades. New zap volleys are held for this long. */
    public static final int PULSE_TTL = 40;

    /**
     * Seed-cube scale for the core of {@code band}. {@link #SEED_CUBE_SCALE} is the <em>band-1</em> base (sized to fill
     * the 5x5x5 Compression Manipulator's 3x3x3 air interior); every higher band has a roomier chamber - the compressor
     * shell is {@code 2*index+1} blocks a side - so the render grows with the band index: band 2 draws the cube at 2x,
     * band 3 at 3x, and so on, instead of a band-1 cube looking lost inside a bigger multiblock. Clamped to at least
     * the base, so band 0 (single block) and the generator cores (which never use this render path) are unaffected.
     */
    public static float seedCubeScale(CompressionBand band) {
        return SEED_CUBE_SCALE * Math.max(1, band.index());
    }

    /**
     * How much the completion-flash <em>radii</em> grow for {@code band}. The flash has to stay inside the sealed
     * chamber, so it tracks the chamber rather than the cube: the interior air is {@code 2r-1} blocks a side (half-width
     * {@code r-0.5}) for compressor radius {@code r}, giving band 1 = {@code 1.0}, band 2 = {@code 1.667}, band 3 =
     * {@code 2.333}. The earlier figure scaled by the cube's index instead, which grew faster than the box did - so on
     * band 2 the doubled spokes punched clean out through the glass. Note only the shell the flash expands across uses
     * this factor; {@link com.remag.rcc.client.CompressionCoreRenderer} keeps each spoke's <em>length</em> fixed, so a
     * higher band gets a proportionally-larger-but-still-contained flash, never a longer-reaching one.
     */
    public static float effectGrowth(CompressionBand band) {
        double chamber = Math.max(1, band.compressorRadius()) - 0.5D;   // interior air half-width for this band
        double base = CompressionBand.BAND_1.compressorRadius() - 0.5D; // 1.5 -> band 1 factor is exactly 1.0
        return (float) (chamber / base);
    }

    /**
     * Craft time, in ticks, for a Compression Manipulator core of {@code band}. Each band above the first takes
     * {@link #CRAFT_SLOWDOWN_PER_BAND}x longer per craft than the one below, so band 1 is the
     * {@link #TICKS_PER_CRAFT} base (12s today), band 2 is 1.5x (18s), band 3 1.5^2 (27s), ... The exponent is
     * {@code index - 1} because band 1 - the first multiblock - is the reference tier. Everything scales off that one
     * base constant by multiplication, so raising the starting speed never means chasing a pile of hardcoded values.
     */
    public static int craftTicks(CompressionBand band) {
        return Math.max(1, (int) Math.round(TICKS_PER_CRAFT * Math.pow(CRAFT_SLOWDOWN_PER_BAND, band.index() - 1)));
    }

    private CompressionConfig() {
    }

    /**
     * Base-10 logarithm of a positive {@link BigInteger}, computed from its decimal digit count and leading
     * mantissa so it stays finite for arbitrarily large tiers.
     */
    public static double log10(BigInteger value) {
        String s = value.toString();
        int head = Math.min(18, s.length());
        double leading = new BigInteger(s.substring(0, head)).doubleValue();
        return Math.log10(leading) + (s.length() - head);
    }

    /**
     * The {@link #TINT_ANCHOR_ARGB} segment a tier's {@code log10} falls in: the index whose stop is the last one at
     * or below it, clamped so anything past the final band ceiling keeps the last segment's end colour.
     */
    private static int tintSegment(double exponent) {
        int last = Math.min(TINT_STOPS.length, TINT_ANCHOR_ARGB.length) - 2;
        for (int i = last; i >= 1; i--) {
            if (exponent > TINT_STOPS[i]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * ARGB tint colour for a tier, from the band-indexed ramp in {@link #TINT_ANCHOR_ARGB}: linear interpolation
     * inside whichever segment {@link #TINT_STOPS} places the tier in. The first two segments are grayscale (so the
     * shipped bands behave like the old curve, just stretched over their own band), and every segment after that
     * pushes a hue - which is only visible because the base sprite is a brightened derivative (see
     * {@link #TINT_BASE_GREY}).
     */
    public static int tintArgb(BigInteger level) {
        double exponent = log10(level.max(BigInteger.ONE));
        int segment = tintSegment(exponent);
        double span = TINT_STOPS[segment + 1] - TINT_STOPS[segment];
        float t = span <= 0.0D ? 1.0F : (float) Math.clamp((exponent - TINT_STOPS[segment]) / span, 0.0D, 1.0D);
        int from = TINT_ANCHOR_ARGB[segment];
        int to = TINT_ANCHOR_ARGB[segment + 1];
        int r = Math.round(((from >> 16) & 0xFF) + t * (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)));
        int g = Math.round(((from >> 8) & 0xFF) + t * (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)));
        int b = Math.round((from & 0xFF) + t * ((to & 0xFF) - (from & 0xFF)));
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    /** log10 of the base-cobblestone-equivalent mass of a tier-{@code t} stack: {@code (t-1) * log10(9)}. */
    public static double tierMassLog(BigInteger tier) {
        return tier.subtract(BigInteger.ONE).doubleValue() * LOG9;
    }

    /**
     * Required fuel mass (log10 space) for a cycle seeded by tier {@code n}: one seed-worth of base cobblestone,
     * i.e. exactly the mass the seed block itself already represents ({@code 9^(n-1)}). Shared by the GUI
     * readout and the {@code canStart} gate so the number shown is the number actually required.
     */
    public static double requiredMassLog(BigInteger n) {
        return tierMassLog(n);
    }

    /**
     * log10 mass of a tier-{@code t} block measured in <em>vanilla</em> cobblestone (BCE), where the base unit is a
     * single cobblestone (tier 0) and each tier is {@code 9x} the previous: {@code 9^t}. This matches the in-world
     * recipe (a tier-1 block is the 3x3 of 9 cobblestone), unlike {@link #tierMassLog} which treats a tier-1 block
     * itself as the unit. Used by the Auto-Compressor, whose ladder starts at tier 0 (a plain cobblestone seed).
     */
    public static double bceMassLog(BigInteger tier) {
        return tier.doubleValue() * LOG9;
    }

    /**
     * Fuel mass (log10 BCE) a single Auto-Compressor {@code t -> t+1} step consumes. The finished tier-{@code t+1}
     * block is worth {@code 9^(t+1)}, but the tier-{@code t} seed already contributes {@code 9^t}, so the pool only
     * supplies the difference: {@code 9^(t+1) - 9^t = 8 * 9^t}, i.e. {@code LOG8 + bceMassLog(t)}. A tier-0 vanilla
     * seed therefore costs {@code 8 * 9^0 = 8} BCE to become its first tier-1 block (9 cobblestone in a 3x3).
     */
    public static double requiredStepMassLog(BigInteger t) {
        return LOG8 + bceMassLog(t);
    }

    /**
     * Drain {@code sub} (a log10 mass) from {@code pool} (a log10 mass): {@code log10(10^pool - 10^sub)}. Returns
     * {@link Double#NEGATIVE_INFINITY} when the subtraction empties the pool ({@code sub >= pool}, within a small
     * epsilon) so an exact drain lands back on the empty-pool sentinel rather than {@code NaN}.
     */
    public static double log10subtract(double pool, double sub) {
        if (pool == Double.NEGATIVE_INFINITY) {
            return Double.NEGATIVE_INFINITY;
        }
        // sub >= pool (allowing a tiny float slop) means the whole pool is consumed.
        if (sub >= pool - 1.0E-9D) {
            return Double.NEGATIVE_INFINITY;
        }
        return pool + Math.log10(1.0 - Math.pow(10.0, sub - pool));
    }

    /**
     * Accumulate two mass logs: {@code log10(10^x + 10^y)}, stable against overflow and large gaps.
     *
     * <p><b>What this cannot do.</b> The result is always within {@code log10(2) = 0.301} of the larger argument, and
     * a {@code double} only resolves changes of about {@code 2.2e-16} <em>relative</em> to its magnitude - so the
     * addition lands while the pool is small and stops landing entirely once the pool's own magnitude grows past
     * roughly {@code 1.4e15} (band 2's rate figure has an ulp of {@code 8192}; band 8's is {@code 9.7e83}). Repeatedly
     * adding a slice is therefore a way to fill a small buffer, <b>not</b> a way to bank output: at the later bands N
     * seconds of minting and 1 second of minting are the same {@code double}. Anything that has to scale with towers
     * or with time does it in exponent space instead - see {@link CompressionBand#shareOf(BigInteger)} and
     * {@link com.remag.rcc.energy.BceNetwork}.
     */
    public static double log10add(double x, double y) {
        if (x == Double.NEGATIVE_INFINITY) {
            return y;
        }
        if (y == Double.NEGATIVE_INFINITY) {
            return x;
        }
        double max = Math.max(x, y);
        double min = Math.min(x, y);
        return max + Math.log10(1.0 + Math.pow(10.0, min - max));
    }
}
