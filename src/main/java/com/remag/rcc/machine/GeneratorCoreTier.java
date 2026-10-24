package com.remag.rcc.machine;

import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * One rung of the <em>tier-1</em> generator ladder - the block-state value that decides what a
 * {@link GeneratorCoreBlockEntity}'s 3x3x4 multiblock mints and what it costs. The tier-0 ladder
 * ({@link GeneratorTier}) tops out at {@code 9^500} BCE/sec - half a tier-1000 block's BCE on the log scale, i.e.
 * the geometric half of the single-block band's ceiling. This ladder takes that handover literally: it starts at
 * the full tier-0 max ({@code 9^1000} BCE/sec, one tier-1000 block per second) and steps in powers of ten up to
 * the geometric half of the tier-1 max block ({@code 9^(5*10^11)} BCE/sec = half of {@code 9^(10^12)} on the log
 * scale, the same {@code sqrt}-of-the-band convention rung 500 uses).
 *
 * <p>Every rung is a pure BCE producer - these rates have long since stopped being countable in items - so unlike
 * {@link GeneratorTier} there is no item/BCE mode split. What <em>is</em> new is an escalating FE bill: a tier-1
 * generator burns FE while minting, doubling with every rung
 * ({@link CompressionConfig#GEN1_BASE_ENERGY_PER_TICK} {@code << ordinal}), making power infrastructure the gate
 * that pipe logistics were on the tier-0 ladder. The FE buffer is sized per rung as one second's draw
 * ({@link #energyCapacity()}), so supply infrastructure has to grow with the generator, not merely exist.
 *
 * <p>Serialised names carry the exponent in short form ({@code t1_p1000} for the {@code 1000} start,
 * {@code t1_10k} .. {@code t1_100b} for the powers of ten, {@code t1_half_band} for the top) so block-state JSON
 * stays readable; the creative label uses {@link #displaySuffix()}.
 */
public enum GeneratorCoreTier implements StringRepresentable {
    T1_P1000(1_000L, "t1_p1000"),
    T1_10K(10_000L, "t1_10k"),
    T1_100K(100_000L, "t1_100k"),
    T1_1M(1_000_000L, "t1_1m"),
    T1_10M(10_000_000L, "t1_10m"),
    T1_100M(100_000_000L, "t1_100m"),
    T1_1B(1_000_000_000L, "t1_1b"),
    T1_10B(10_000_000_000L, "t1_10b"),
    T1_100B(100_000_000_000L, "t1_100b"),
    T1_HALF_BAND(500_000_000_000L, "t1_half_band");

    /** The {@code 9^n} exponent of this rung's per-second BCE output; exact in a {@code long} up to the top rung. */
    private final long tierExponent;
    private final String name;

    GeneratorCoreTier(long tierExponent, String name) {
        this.tierExponent = tierExponent;
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public long tierExponent() {
        return this.tierExponent;
    }

    /**
     * log10 of this rung's per-second output, in BCE: {@code exponent * log10(9)}. Below {@code 2^53} the exponent
     * is exact in a {@code double}, so even the top rung ({@code 5*10^11}) maps to a faithful ~4.77e11 log mass.
     */
    public double rateLog10PerSec() {
        return this.tierExponent * CompressionConfig.LOG9;
    }

    /** log10 of the per-tick addition (20 ticks to a second): {@code rateLog - log10(20)}. */
    public double rateLog10PerTick() {
        return rateLog10PerSec() - Math.log10(20.0D);
    }

    /** Buffer ceiling (log10 BCE) - half a second's own output, the tier-0 stall convention carried over. */
    public double bufferCeilingLog() {
        return rateLog10PerSec() - Math.log10(2.0D);
    }

    /** FE/tick burned while minting; doubles with every rung up the ladder. */
    public int energyPerTick() {
        return CompressionConfig.GEN1_BASE_ENERGY_PER_TICK << ordinal();
    }

    /** FE buffer ceiling: exactly one second of this rung's draw, so supply has to keep pace rung by rung. */
    public int energyCapacity() {
        return energyPerTick() * 20;
    }

    /** A compact readout for chat/tooltip use: {@code "9^500,000,000,000 BCE/sec"}. */
    public String describe() {
        return "9^" + String.format(java.util.Locale.US, "%,d", tierExponent) + " BCE/sec";
    }

    /** The creative-label suffix: {@code "I"} .. {@code "X"}, so the ten rungs read as one ordered line. */
    public String displaySuffix() {
        return DisplayNames.roman(ordinal() + 1);
    }

    /** Lookup by the block-state serial name (reading a placed stack's {@code block_state} component). */
    @Nullable
    public static GeneratorCoreTier byName(String name) {
        for (GeneratorCoreTier tier : values()) {
            if (tier.name.equals(name)) {
                return tier;
            }
        }
        return null;
    }
}
