package com.remag.rcc.registry;

import java.math.BigInteger;

/**
 * The single knob for the whole mod: how the compression tiers are bounded, the darkness curve that maps a
 * tier to a tint brightness, and the shared math for the big-tier machine's base-cobblestone-equivalent mass.
 *
 * <p>Tiers are {@link BigInteger} and represent an <em>emergent</em> amount of cobblestone: a tier-{@code t}
 * block is equivalent to {@code 9^(t-1)} base cobblestone, but we never materialise that count. Anything that
 * needs a comparable magnitude works in base-10 log space (a {@code double}), which stays finite and monotonic
 * even toward a googolplex-tier block.
 */
public final class CompressionConfig {
    // --- Tier bounds ---------------------------------------------------------------------------
    /** Highest tier the vanilla 3x3 compress chain can produce; also the creative-tab listing ceiling. */
    public static final BigInteger MANUAL_MAX_LEVEL = BigInteger.valueOf(1000);
    /** Lowest seed tier the Compression Manipulator will accept. */
    public static final BigInteger MANIPULATOR_MIN_IN = BigInteger.valueOf(1000);
    /** Highest output tier this machine will produce (a future machine continues the ladder). */
    public static final BigInteger MANIPULATOR_MAX_OUT = BigInteger.TEN.pow(12);

    // --- Visual curve --------------------------------------------------------------------------
    /** log10 ceiling the tint ramp is normalised against (tier 1 -> white, ~1e12 -> black). */
    public static final int TINT_EXPONENT_CEILING = 12;

    /** log10(8): the extra base-cobblestone mass a single +1 tier step needs beyond the seed it upgrades. */
    public static final double LOG8 = Math.log10(8d);

    // --- Machine / mass math -------------------------------------------------------------------
    /** log10(9): the base-cobblestone multiplier each compression tier adds. */
    public static final double LOG9 = Math.log10(9d);

    // --- Manipulator multiblock ---------------------------------------------------------------
    public static final int REGION_RADIUS = 2; // 5x5x5 cube centred on the core
    public static final int ENERGY_CAPACITY = 1_000_000;
    public static final int ENERGY_PER_CRAFT = 250_000;
    public static final int TICKS_PER_CRAFT = 240; // 120s per cycle (progress bar + drain rate derive from this)
    public static final int MIN_POWER_PORTS = 1;
    public static final int MIN_ITEM_IN_PORTS = 1;
    public static final int MIN_ITEM_OUT_PORTS = 1;
    /** The structure needs exactly one Interface block (the only GUI entry point); more makes no sense. */
    public static final int MIN_INTERFACES = 1;
    public static final int MAX_INTERFACES = 1;
    /**
     * Ceiling of the Manipulator's BCE-as-fuel intake (the ITEM_IN port's sink view), in log10 BCE. Set effectively
     * unlimited ({@code 1e18}) because the pool is a logarithm and this machine runs at the giant tiers
     * ({@link #MANIPULATOR_MIN_IN} .. {@link #MANIPULATOR_MAX_OUT}) whose fuel needs already reach ~1e12 log10 mass -
     * anything smaller would reject legitimate feeds. Mirrors {@link #DECOMP_BCE_CAPACITY_LOG} on the output side.
     */
    public static final double MANIPULATOR_BCE_CAPACITY_LOG = 1.0E18D;

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
     * Hard ceiling of the Auto Decompressor's BCE buffer, in log10 BCE. Deliberately enormous - far above any mass
     * the game can actually produce (the multiblock tops out near tier 1e12, i.e. a ~1e12 log10 mass) - so the buffer
     * is <em>effectively unlimited</em> and a descent can never stall for lack of room. Only a pipe that actually
     * drains it can keep the pool low; until then the tank is a one-way vault. Safe because the value is already a
     * logarithm, so {@code 1e18} costs a double nothing and {@link #log10add} cannot overflow on it.
     *
     * <p><b>Temporary:</b> this is only open-ended because nothing can drain the tank yet. Once BCE logistics exist
     * (pipes moving it out, machines burning it as fuel) bring this back down to a real storage figure so the buffer
     * is a meaningful constraint again.
     */
    public static final double DECOMP_BCE_CAPACITY_LOG = 1.0E18D;
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
    public static final int DECOMP_REGION_RADIUS = 1;
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
    public static final int GEN1_BASE_ENERGY_PER_TICK = 1_000;
    /**
     * Half-width of the tier-1 generator's horizontal shell ({@code 3x3} columns) - same ring rule as the 3x3x3
     * decompressor - while the column runs four layers tall ({@link #GEN1_LAYERS_BELOW} under and
     * {@link #GEN1_LAYERS_ABOVE} over the core).
     */
    public static final int GEN1_REGION_RADIUS = 1;
    /** Layers under the core when it seals the <em>lower</em> interior slot of the four-tall column. */
    public static final int GEN1_LAYERS_BELOW = 1;
    /**
     * Layers above the core when it seals the <em>lower</em> interior slot (mirrored when it seals the upper one).
     * The two interior slots are core + 1x1 air shaft; the scan tries both readings of the column.
     */
    public static final int GEN1_LAYERS_ABOVE = 2;
    /** The generator multiblock needs at least one POWER port... */
    public static final int MIN_GEN1_POWER_PORTS = 1;
    /** ...and <em>exactly</em> one BCE-out (ITEM_OUT) port, so the mint rate never splits across two drains. */
    public static final int EXACT_GEN1_BCE_OUT_PORTS = 1;

    // --- BCE logistics: pipes + storage (see energy.BceNetwork, machine.BcePipeBlock) --------------------------
    /**
     * Throughput of each BCE pipe tier as an <em>absolute per-transfer ceiling</em>, in log10 BCE, indexed by tier
     * ordinal. A pipe moves at most this much and at most whatever its source holds, whichever is smaller - so the
     * two tiers map onto the two machine bands: tier I tops out at one tier-{@link #MANUAL_MAX_LEVEL} block's BCE
     * (the Auto-Compressor band), tier II at one tier-{@link #MANIPULATOR_MAX_OUT} block's BCE (the Manipulator band).
     *
     * <p>An absolute ceiling replaced the old fraction-of-source rate because the bands are now fixed: tying a tier to
     * its machine's biggest block makes the pipe's job legible ("drain one tier-1000 block per transfer") and, unlike
     * a percentage, lets a huge source still feed a much smaller sink instead of the two orders of magnitude refusing
     * to meet. Values derive straight from the band constants so they stay tunable in one place.
     */
    public static final double[] BCE_PIPE_MAX_TRANSFER_LOG = {bceMassLog(MANUAL_MAX_LEVEL), bceMassLog(MANIPULATOR_MAX_OUT)};
    /**
     * The smallest transfer a log10 buffer can physically express. Doubles carry ~15 significant digits, so at
     * magnitude ~954 anything more than ~13 orders smaller than the pool no-ops on add/subtract - moving such a
     * slice would silently create a hair of mass. <strong>No longer a transfer gate:</strong> {@code BceNetwork}
     * runs every leg unconditionally and accepts that bounded, invisible sliver (the end goal is so many orders away
     * that the duplication is nothing). This constant now only sizes a store's headroom above its nominal capacity
     * (see {@link #AUTO_BCE_CAPACITY_LOG}), so a sink always has room to express the last slice of a transfer.
     */
    public static final double BCE_TRANSFER_FLOOR_LOG = 10.0D;
    /** Capacity of the single-block BCE storage tank, in log10 BCE: one tier-{@code 1000} block's full yield. */
    public static final double BCE_STORAGE_CAPACITY_LOG = bceMassLog(AUTO_TARGET_LEVEL);
    /**
     * Capacity of the <em>tier-two</em> tank, in log10 BCE: one tier-{@link #MANIPULATOR_MAX_OUT} block's full yield
     * - the biggest thing the multiblock band (Compression / Decompression Manipulator) can make or release. It is
     * deliberately {@code BCE_PIPE_MAX_TRANSFER_LOG[1]} on the transfer side: one tier-II pipe push fits the whole
     * tank, so the multiblock's mass has a vault that neither the tier-I pipe band nor the original tank (sized to
     * the tier-1000 Auto band) can strain.
     */
    public static final double BCE_STORAGE_2_CAPACITY_LOG = bceMassLog(MANIPULATOR_MAX_OUT);
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
     * Ceiling of the Auto-Compressor's BCE fuel pool, in log10 BCE: one full storage tank's yield plus ten orders of
     * margin. The margin is not decoration - a sink whose ceiling equals its biggest possible input leaves nothing for
     * {@link #log10subtract} to express as "room", so the last slice of every transfer would be refused forever; ten
     * decades is exactly {@link #BCE_TRANSFER_FLOOR_LOG}, the smallest difference this space can talk about. Item
     * fuel is still uncapped against this ({@code acceptFuel} just accumulates), so the number only gates what a bus
     * is allowed to push in.
     */
    public static final double AUTO_BCE_CAPACITY_LOG = BCE_STORAGE_CAPACITY_LOG + BCE_TRANSFER_FLOOR_LOG;

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
     * Buffer ceiling of a BCE generator, in log10 BCE: half a second's worth of its own steady-state production
     * ({@code rateLog - log10(2)}). A full buffer stalls the generator until a pipe drains it, which is what makes each
     * generator a gate - you must build the next-tier drain infrastructure (and the next generator) to keep mass moving.
     */
    public static double generatorBufferCeilingLog(int tierExponent) {
        return generatorRateLog10PerSec(tierExponent) - Math.log10(2.0D);
    }

    // --- Seed cube render (the spinning cube drawn at the core; see client.CompressionCoreRenderer) ---------
    /**
     * Side length, in blocks, of the seed cube. The core is a unit cube whose corners reach {@code sqrt(3)/2 ~ 0.87};
     * a concentric cube only hides it from <em>every</em> rotation once its inradius (scale/2) reaches that, i.e.
     * {@code SEED_CUBE_SCALE >= sqrt(3)}. Keep it under 3 so the tumbling corners stay inside the 3x3x3 air interior.
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
     * Brightness multiplier for a tier, in {@code [0, 1]}: a log ramp so tier 1 is only slightly darker than
     * vanilla and the {@link #TINT_EXPONENT_CEILING} tier reaches {@code 0f} (fully dark).
     */
    public static float brightness(BigInteger level) {
        double exponent = log10(level.max(BigInteger.ONE));
        float b = (float) (1.0 - exponent / TINT_EXPONENT_CEILING);
        return Math.clamp(b, 0f, 1f);
    }

    /**
     * ARGB tint colour for a tier: a pure grayscale multiply so the base texture only darkens (never shifts
     * hue).
     */
    public static int tintArgb(BigInteger level) {
        int channel = Math.round(255f * brightness(level));
        channel = Math.clamp(channel, 0, 255);
        return (0xFF << 24) | (channel << 16) | (channel << 8) | channel;
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

    /** Accumulate two mass logs: {@code log10(10^x + 10^y)}, stable against overflow and large gaps. */
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
