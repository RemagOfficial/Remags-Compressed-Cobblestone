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

    // --- Machine / mass math -------------------------------------------------------------------
    /** log10(9): the base-cobblestone multiplier each compression tier adds. */
    public static final double LOG9 = Math.log10(9d);

    // --- Manipulator multiblock ---------------------------------------------------------------
    public static final int REGION_RADIUS = 2; // 5x5x5 cube centred on the core
    public static final int ENERGY_CAPACITY = 1_000_000;
    public static final int ENERGY_PER_CRAFT = 250_000;
    public static final int TICKS_PER_CRAFT = 2400; // 120s per cycle (progress bar + drain rate derive from this)
    public static final int MIN_POWER_PORTS = 1;
    public static final int MIN_ITEM_IN_PORTS = 1;
    public static final int MIN_ITEM_OUT_PORTS = 1;
    /** The structure needs exactly one Interface block (the only GUI entry point); more makes no sense. */
    public static final int MIN_INTERFACES = 1;
    public static final int MAX_INTERFACES = 1;

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
