package com.remag.rcc.machine;

import com.remag.rcc.registry.BceNotation;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * One rung of a generation manipulator's ladder - the {@code (band, index)} pair that decides what a
 * {@link GeneratorCoreBlockEntity}'s tower mints and what it costs. It used to be a fixed enum of ten band-1
 * constants; it is now resolved from a {@link CompressionBand} row and the rung's position within that row's
 * {@link CompressionBand#rungExponents() ladder}, so a new band's ladder is drawn from the table with no new code
 * (exactly what {@link GeneratorCoreBlock#RUNG} storing a bounded {@code int} index exists to allow). The band-0
 * single-block ladder stays on {@link GeneratorTier}; this class is the multiblock family, which starts at band 1.
 *
 * <p>Every band's ladder holds the same number of rungs ({@link CompressionBand#RUNGS_PER_BAND}), so climbing one tier
 * feels like climbing the next: the rungs are the band's start - the previous band's ceiling - then evenly spaced steps
 * in log space, topped by the geometric half of the band's own ceiling. Those endpoints come from
 * {@link CompressionBand#rungStartExponent()} / {@link CompressionBand#rungEndExponent()}, so band 1 runs {@code
 * 1000 .. 5*10^14} and band 2 inherits {@code 10^15 .. 5*10^26} without this class changing.
 *
 * <p><b>A rung is a tower's share, not a bus's output.</b> What one tower of this band deposits is
 * {@link #shareExponent()} - the rung exponent divided by {@code 2^band} ({@link CompressionBand#shareOf}) - so a
 * band-1 tower mints half of what its rung names and {@link CompressionBand#arrayFullOutputTowers() four} of them on
 * one BCE bus mint a whole tier-{@code 10^15} block per second. That is the {@code MASTER_PLAN.md} D23 array rule, and
 * it exists because summing <em>masses</em> is unrepresentable at these magnitudes (see
 * {@link CompressionConfig#log10add}) while summing {@link BigInteger} exponents is exact. {@link #rateLog10PerSec()},
 * {@link #rateTower()} and {@link #describe()} all report the share, so the figure the player is shown is the figure
 * delivered. {@link #tierExponent()} remains the rung's <em>milestone</em> - the tier whose block gates the upgrade
 * recipe and the advancement - which is why climbing is still about reaching blocks, not about bigger mint numbers.
 *
 * <p>Every rung is a pure BCE producer - these rates have long since stopped being countable in items - so unlike
 * {@link GeneratorTier} there is no item/BCE mode split. What <em>is</em> new is an escalating FE bill: a generator
 * burns FE while minting, doubling with every rung ({@link CompressionBand#baseEnergyPerTick()} {@code << index}),
 * making power infrastructure the gate that pipe logistics were on the tier-0 ladder. The FE buffer is sized per rung
 * as one second's draw ({@link #energyCapacity()}), so supply infrastructure has to grow with the generator, not
 * merely exist. Because FE is an {@code int} currency, both figures saturate at {@link Integer#MAX_VALUE} rather than
 * wrapping - a later band that wants to bill more than that is meant to add POWER ports, not bigger numbers.
 *
 * <p>Serialised names carry the handover tier for the first rung ({@code t1_p1000} for the {@code 1000} start) and a
 * fixed {@code t1_half_band} for the top, but the rungs in between are keyed by <em>position</em> ({@code t1_2} ..
 * {@code t1_12}): their tiers are rounded to whole powers of ten, so a descriptive name could collide with a neighbour
 * on a band whose steps are tighter than a decade. The {@code t<band>} tag keeps a later band's rungs from colliding
 * with band 1's.
 */
public final class GeneratorCoreTier {
    private final CompressionBand band;
    private final int index;
    private final BigInteger tierExponent;
    private final String name;

    /**
     * Resolve rung {@code index} of {@code band}'s ladder. The index is clamped into the band's own rung list so a
     * block state read from a foreign (higher) index can never run off the end of a narrower band's ladder.
     */
    public GeneratorCoreTier(CompressionBand band, int index) {
        List<BigInteger> rungs = band.rungExponents();
        int clamped = Math.min(Math.max(index, 0), Math.max(0, rungs.size() - 1));
        this.band = band;
        this.index = clamped;
        this.tierExponent = rungs.isEmpty() ? BigInteger.ZERO : rungs.get(clamped);
        this.name = computeName(band, clamped, this.tierExponent, rungs.size());
    }

    /** Every rung of {@code band}'s ladder, in climbing order - what the creative tab and recipe line iterate. */
    public static List<GeneratorCoreTier> rungsFor(CompressionBand band) {
        List<GeneratorCoreTier> rungs = new ArrayList<>();
        List<BigInteger> exponents = band.rungExponents();
        for (int i = 0; i < exponents.size(); i++) {
            rungs.add(new GeneratorCoreTier(band, i));
        }
        return rungs;
    }

    /**
     * Serial form of one rung. The band's handover rung keeps its readable tier ({@code p1000}), the top is always
     * {@code half_band}, and everything in between is numbered by position - the rungs between are rounded powers of
     * ten whose short names can collide on a tight band, while the index cannot.
     */
    private static String computeName(CompressionBand band, int index, BigInteger exponent, int size) {
        String bandTag = "t" + band.index();
        if (index == 0) {
            return bandTag + "_p" + shortName(exponent);
        }
        if (index == size - 1) {
            return bandTag + "_half_band";
        }
        return bandTag + "_" + (index + 1);
    }

    /**
     * Short readable form of a power-of-ten tier ({@code 1000}, {@code 10k}, {@code 100m}, {@code 1t}, ..). Only the
     * ladder's first rung still needs it - that is the previous band's ceiling, always a clean power of ten.
     */
    private static String shortName(BigInteger value) {
        int e = value.toString().length() - 1; // value is 10^e, whose decimal form has e + 1 digits
        if (e <= 3) {
            return BigInteger.TEN.pow(e).toString();
        }
        String[] suffixes = {"", "k", "m", "b", "t", "q", "x"}; // all lowercase - these feed resource paths
        int group = e / 3;
        int within = e % 3;
        String mantissa = within == 0 ? "1" : BigInteger.TEN.pow(within).toString();
        String suffix = group < suffixes.length ? suffixes[group] : "e" + (3 * group);
        return mantissa + suffix;
    }

    /**
     * The rung's short serial name ({@code t1_p1000} .. {@code t1_half_band}), used for recipe / advancement ids and
     * lang keys. Not a block-state value any more - {@link GeneratorCoreBlock#RUNG} is now a plain index - so it is
     * purely a readable, stable handle for one position in one band's ladder.
     */
    public String getSerializedName() {
        return this.name;
    }

    /** This rung's <em>milestone</em> exponent: the compression tier whose block gates the upgrade recipe. */
    public BigInteger tierExponent() {
        return this.tierExponent;
    }

    /** The band this rung ladder belongs to, so the bus can clamp an array at the right ceiling. */
    public CompressionBand band() {
        return this.band;
    }

    /** This rung's zero-based position within its band's ladder (the value carried in {@code RUNG}). */
    public int index() {
        return this.index;
    }

    /**
     * The exponent this tower contributes to its bus: {@link #tierExponent()} divided by {@code 2^band}
     * ({@link CompressionBand#shareOf}). Band 1 halves every rung, so four rung-X towers are needed for the
     * {@code 9^(2X)}/sec a whole tier-{@code 2X} block per second means; the sum happens in
     * {@link com.remag.rcc.energy.BceNetwork}.
     */
    public BigInteger shareExponent() {
        return this.band.shareOf(this.tierExponent);
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
        long base = this.band.baseEnergyPerTick();
        int shifts = this.index;
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
     * reaches {@code 10^15} on band 1 and {@code 10^27} on the next, so it is written as a stack ({@code 9^(10¹⁵)})
     * rather than as digits - {@link com.remag.rcc.item.GeneratorCoreItem} draws it, {@link #describe()} flattens it.
     */
    public BceNotation.Tower rateTower() {
        return BceNotation.power("9", shareExponent());
    }

    /** The creative-label suffix: {@code "I"} .. the rung's roman number, so the ladder reads as one ordered line. */
    public String displaySuffix() {
        return DisplayNames.roman(this.index + 1);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GeneratorCoreTier tier
                && this.band == tier.band && this.index == tier.index;
    }

    @Override
    public int hashCode() {
        return 31 * this.band.hashCode() + this.index;
    }
}
