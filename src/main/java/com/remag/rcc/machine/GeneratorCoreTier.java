package com.remag.rcc.machine;

import com.remag.rcc.registry.BceNotation;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

/**
 * One rung of the <em>tier-1</em> generator ladder - the block-state value that decides what a
 * {@link GeneratorCoreBlockEntity}'s 3x3x4 multiblock mints and what it costs. The tier-0 ladder
 * ({@link GeneratorTier}) tops out at {@code 9^500} BCE/sec - half a tier-1000 block's BCE on the log scale, i.e.
 * the geometric half of the single-block band's ceiling. This ladder takes that handover literally: it starts at
 * the full tier-0 max ({@code 9^1000} BCE/sec, one tier-1000 block per second) and steps in powers of ten up to
 * the geometric half of the tier-1 max block ({@code 9^(5*10^11)} BCE/sec = half of {@code 9^(10^12)} on the log
 * scale, the same {@code sqrt}-of-the-band convention rung 500 uses).
 *
 * <p><b>A rung is a tower's share, not a bus's output.</b> What one tower of this band deposits is
 * {@link #shareExponent()} - the rung exponent divided by {@code 2^band} ({@link CompressionBand#shareOf}) - so a
 * band-1 tower mints half of what its rung names and {@link CompressionBand#arrayFullOutputTowers() four} of them on
 * one BCE bus mint a whole tier-{@code 10^12} block per second. That is the {@code MASTER_PLAN.md} D23 array rule, and
 * it exists because summing <em>masses</em> is unrepresentable at these magnitudes (see
 * {@link CompressionConfig#log10add}) while summing {@link BigInteger} exponents is exact. {@link #rateLog10PerSec()},
 * {@link #rateTower()} and {@link #describe()} all report the share, so the figure the player is shown is the figure
 * delivered. {@link #tierExponent()} remains the rung's <em>milestone</em> - the tier whose block gates the upgrade
 * recipe and the advancement - which is why climbing is still about reaching blocks, not about bigger mint numbers.
 *
 * <p>Every rung is a pure BCE producer - these rates have long since stopped being countable in items - so unlike
 * {@link GeneratorTier} there is no item/BCE mode split. What <em>is</em> new is an escalating FE bill: a tier-1
 * generator burns FE while minting, doubling with every rung
 * ({@link CompressionConfig#GEN1_BASE_ENERGY_PER_TICK} {@code << ordinal}), making power infrastructure the gate
 * that pipe logistics were on the tier-0 ladder. The FE buffer is sized per rung as one second's draw
 * ({@link #energyCapacity()}), so supply infrastructure has to grow with the generator, not merely exist. Because FE
 * is an {@code int} currency, both figures saturate at {@link Integer#MAX_VALUE} rather than wrapping - a later band
 * that wants to bill more than that is meant to add POWER ports, not bigger numbers.
 *
 * <p>The ladder's endpoints are read from {@link CompressionBand#BAND_1} rather than hardcoded, so a new band's
 * rung list starts and stops where the band table says it must.
 *
 * <p>Serialised names carry the exponent in short form ({@code t1_p1000} for the {@code 1000} start,
 * {@code t1_10k} .. {@code t1_100b} for the powers of ten, {@code t1_half_band} for the top) so block-state JSON
 * stays readable; the creative label uses {@link #displaySuffix()}.
 */
public enum GeneratorCoreTier implements StringRepresentable {
    T1_P1000(CompressionBand.BAND_1.rungStartExponent(), "t1_p1000"),
    T1_10K(BigInteger.TEN.pow(4), "t1_10k"),
    T1_100K(BigInteger.TEN.pow(5), "t1_100k"),
    T1_1M(BigInteger.TEN.pow(6), "t1_1m"),
    T1_10M(BigInteger.TEN.pow(7), "t1_10m"),
    T1_100M(BigInteger.TEN.pow(8), "t1_100m"),
    T1_1B(BigInteger.TEN.pow(9), "t1_1b"),
    T1_10B(BigInteger.TEN.pow(10), "t1_10b"),
    T1_100B(BigInteger.TEN.pow(11), "t1_100b"),
    T1_HALF_BAND(CompressionBand.BAND_1.rungEndExponent(), "t1_half_band");

    /**
     * The band this whole ladder belongs to - every constant above is a band-1 rung. A band-2 ladder passes its own
     * row here and inherits the share divisor, the array size and the ceilings from the table. Read from method bodies
     * only: enum constants are constructed before this field is assigned.
     */
    private static final CompressionBand BAND = CompressionBand.BAND_1;

    /**
     * The {@code 9^n} <em>milestone</em> exponent of this rung - the tier whose block's BCE equals the rung's nominal
     * per-second figure, and the block its upgrade recipe asks for. A {@link BigInteger}, not a {@code long}: the
     * shipped band's top rung ({@code 5*10^11}) fits a long easily, but band 2's ladder ends at {@code 5*10^19},
     * past {@link Long#MAX_VALUE}, so the rung type must not be what caps a band.
     */
    private final BigInteger tierExponent;
    private final String name;

    GeneratorCoreTier(BigInteger tierExponent, String name) {
        this.tierExponent = tierExponent;
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * This rung's <em>milestone</em> exponent: the compression tier whose block is worth the rung's nominal
     * {@code 9^n}/sec, and therefore the block the upgrade recipe and the advancement ask for. Not what a lone tower
     * deposits - see {@link #shareExponent()}, which is this figure divided by the band's array divisor.
     */
    public BigInteger tierExponent() {
        return this.tierExponent;
    }

    /** The band this rung ladder belongs to, so the bus can clamp an array at the right ceiling. */
    public CompressionBand band() {
        return BAND;
    }

    /**
     * The exponent this tower contributes to its bus: {@link #tierExponent()} divided by {@code 2^band}
     * ({@link CompressionBand#shareOf}). Band 1 halves every rung, so four rung-X towers are needed for the
     * {@code 9^(10^12)}/sec a whole tier-1 block per second means; the sum happens in
     * {@link com.remag.rcc.energy.BceNetwork}.
     */
    public BigInteger shareExponent() {
        return BAND.shareOf(this.tierExponent);
    }

    /**
     * log10 of this tower's per-second output, in BCE: {@code shareExponent * log10(9)}. This is the delivered
     * figure - the lump of {@link CompressionConfig#GEN_MINT_PERIOD_TICKS} ticks - and there is no longer a
     * per-tick variant of it, because a twentieth-of-a-lump is not representable at these magnitudes.
     */
    public double rateLog10PerSec() {
        return CompressionConfig.bceMassLog(shareExponent());
    }

    /**
     * This rung's FE/t demand before the {@code int} squeeze: the band's base draw doubled by the rung's position.
     * Computed in a {@code long} and saturated at {@link Long#MAX_VALUE}, because a shift that overflows an {@code
     * int} would wrap to a <em>negative</em> bill and every consumer below would read a negative cost as free power.
     */
    private long rawEnergyPerTick() {
        long base = CompressionConfig.GEN1_BASE_ENERGY_PER_TICK;
        int shifts = ordinal();
        if (shifts >= Long.SIZE - 1 || base > (Long.MAX_VALUE >> shifts)) {
            return Long.MAX_VALUE;
        }
        return base << shifts;
    }

    /** FE/tick burned while minting; doubles with every rung, capped at what the FE currency can carry. */
    public int energyPerTick() {
        return (int) Math.min(rawEnergyPerTick(), Integer.MAX_VALUE);
    }

    /**
     * FE buffer ceiling: one second of this rung's draw (20 ticks), so supply has to keep pace rung by rung. The
     * multiply is done in a {@code long} for the same reason as the draw, and saturates alongside it - the ratio to
     * {@link #energyPerTick()} is exactly 20 only while neither side is capped.
     */
    public int energyCapacity() {
        long perTick = rawEnergyPerTick();
        long capacity = perTick > Long.MAX_VALUE / 20 ? Long.MAX_VALUE : perTick * 20L;
        return (int) Math.min(capacity, Integer.MAX_VALUE);
    }

    /** A compact readout for chat/tooltip use: {@code 9} with the rung's exponent stacked above it, then {@code BCE/sec}. */
    public String describe() {
        return BceNotation.flat(rateTower()) + " BCE/sec";
    }

    /**
     * This tower's {@code 9^n} share of the band as a drawable power tower. The exponent is a {@link BigInteger} that
     * reaches {@code 10^12} on this ladder and {@code 10^20} on the next, so it is written as a stack ({@code 9^(10¹²)})
     * rather than as digits - {@link com.remag.rcc.item.GeneratorCoreItem} draws it, {@link #describe()} flattens it.
     */
    public BceNotation.Tower rateTower() {
        return BceNotation.power("9", shareExponent());
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
