package com.remag.rcc.registry;

import java.math.BigInteger;

/**
 * The progression <b>bands</b> - one row per tier of machinery the mod ships or plans to ship. This is the single
 * source of truth every machine, pipe, tank and generator should read its numbers from, so that adding a band is
 * <em>one row</em> instead of a new constant in six places (see {@code MASTER_PLAN.md} §4.1).
 *
 * <p>A band is named for the compression tier its compressor tops out at: band {@code k} accepts whatever band
 * {@code k-1} produced ({@link #minInputTier()} is literally the previous row's {@link #maxOutputTier()}, the
 * handover rule the shipped machines already follow - {@link CompressionConfig#MANIPULATOR_MIN_IN} is 1000 because
 * band 0's ceiling is 1000) and outputs blocks up to {@link #maxOutputTier()}. Bands 0 and 1 are <b>shipped</b>;
 * rows 2..8 are the plan and exist here so the geometry, ceilings and scan radii can be generalised <em>before</em>
 * band 2 is written, not after.
 *
 * <p><b>Only bands 0-1 may be trusted as tuned.</b> The later rows carry placeholder numbers on purpose - the
 * per-band FE cost (D5), required port counts (D-ports) and rung ladders are open decisions in
 * {@code MASTER_PLAN.md} §9 - but the <em>geometry</em> rows are the agreed plan: compressor side {@code 2k+3},
 * decompressor side {@code 2k+1} (the family ends at band 3, D7), generator tower {@code (2k+1)} wide and
 * {@code (2k+2)} tall.
 *
 * <p><b>Initialisation order matters.</b> No row's constructor may call {@link CompressionConfig}, because
 * {@code CompressionConfig}'s own static fields read this enum (the shipped constants are aliases onto rows 0-1).
 * Rows therefore store {@link BigInteger} tiers and plain ints only; everything expressed in log10 BCE mass is
 * computed in a method body, where both classes are already initialised.
 */
public enum CompressionBand {
    /** Shipped. Single-block band: manual 3x3 crafting climb plus the Auto machines, ending at tier 1000. */
    BAND_0(BigInteger.TEN.pow(3), 0, 0, 0, 0, 0, 0, 0L),
    /** Shipped. The three 5x5x5 / 3x3x3 / 3x3x4 multiblocks, ending at tier 10^12. */
    BAND_1(BigInteger.TEN.pow(12), 2, 1, 1, 1, 2, 1, 1_000L),
    /** Planned: 7x7x7 compressor, 5x5x5 decompressor (fast descent), 5x5x6 tower. */
    BAND_2(BigInteger.TEN.pow(20), 3, 2, 2, 2, 3, 1, 2_000L),
    /** Planned: 9x9x9 compressor, 7x7x7 <em>instant</em> decompressor (the family's last), 7x7x8 tower. */
    BAND_3(BigInteger.TEN.pow(28), 4, 3, 3, 3, 4, 2, 4_000L),
    /** Planned: from here on no new decompressor exists - band 3's covers every tier (D7). */
    BAND_4(BigInteger.TEN.pow(38), 5, 0, 4, 4, 5, 2, 8_000L),
    /** Planned. */
    BAND_5(BigInteger.TEN.pow(50), 6, 0, 5, 5, 6, 2, 16_000L),
    /** Planned. */
    BAND_6(BigInteger.TEN.pow(64), 7, 0, 6, 6, 7, 3, 32_000L),
    /** Planned. */
    BAND_7(BigInteger.TEN.pow(80), 8, 0, 7, 7, 8, 3, 64_000L),
    /** Planned finale: a block whose compression tier is a googol. */
    BAND_8(BigInteger.TEN.pow(100), 9, 0, 8, 8, 9, 4, 128_000L);

    /** Highest band in the table - the one whose tier ceiling is the mod's stated goal. */
    public static final CompressionBand TOP = values()[values().length - 1];

    private final BigInteger maxOutputTier;
    private final int compressorRadius;
    private final int decompressorRadius;
    private final int generatorHalfWidth;
    private final int generatorLayersBelow;
    private final int generatorLayersAbove;
    private final int minPowerPorts;
    private final long baseEnergyPerTick;

    CompressionBand(BigInteger maxOutputTier, int compressorRadius, int decompressorRadius, int generatorHalfWidth,
                    int generatorLayersBelow, int generatorLayersAbove, int minPowerPorts, long baseEnergyPerTick) {
        this.maxOutputTier = maxOutputTier;
        this.compressorRadius = compressorRadius;
        this.decompressorRadius = decompressorRadius;
        this.generatorHalfWidth = generatorHalfWidth;
        this.generatorLayersBelow = generatorLayersBelow;
        this.generatorLayersAbove = generatorLayersAbove;
        this.minPowerPorts = minPowerPorts;
        this.baseEnergyPerTick = baseEnergyPerTick;
    }

    /** The band index, identical to its ordinal (band 0 = the single-block starting band). */
    public int index() {
        return this.ordinal();
    }

    /** Highest compression tier this band's compressor produces. */
    public BigInteger maxOutputTier() {
        return this.maxOutputTier;
    }

    /**
     * Lowest tier this band's machines will accept: exactly the previous band's ceiling, so the bands butt joint
     * with no gap and no overlap. Band 0 starts at tier 0 - a plain cobblestone.
     */
    public BigInteger minInputTier() {
        return this.index() == 0 ? BigInteger.ZERO : values()[this.index() - 1].maxOutputTier();
    }

    /** True once the band is built from hand-placed shell blocks rather than being a single machine. */
    public boolean hasStructure() {
        return this.compressorRadius > 0;
    }

    /** Half-width of this band's compressor cube (side {@code 2r+1}); {@code 0} for the single-block band. */
    public int compressorRadius() {
        return this.compressorRadius;
    }

    /** Half-width of this band's decompressor cube, or {@code 0} when the band ships none (bands 0 and 4-8, D7). */
    public int decompressorRadius() {
        return this.decompressorRadius;
    }

    /** Whether this band adds a decompressor structure at all (true for bands 1-3 only). */
    public boolean hasDecompressor() {
        return this.decompressorRadius > 0;
    }

    /** Half-width of the generator tower's columns (side {@code 2k+1} for band {@code k}); {@code 0} for band 0. */
    public int generatorHalfWidth() {
        return this.generatorHalfWidth;
    }

    /** Interior layers under the core when it seals the lower slot of the tower's column. */
    public int generatorLayersBelow() {
        return this.generatorLayersBelow;
    }

    /** Interior layers over the core in that reading (mirrored when the core seals the upper slot). */
    public int generatorLayersAbove() {
        return this.generatorLayersAbove;
    }

    /** Total height of the generator tower's column: shell under + the core + shell over. */
    public int generatorHeight() {
        return this.generatorLayersBelow + 1 + this.generatorLayersAbove;
    }

    /**
     * The largest Chebyshev distance any cell of this band's structures can sit from its own core, which is what a
     * host lookup, a capability-cache sweep or a shell-rescan has to cover. The tower is the tall case: its core is
     * off-centre vertically, so {@link #generatorLayersAbove()} can reach further than the horizontal half-width.
     */
    public int shellExtent() {
        int widest = Math.max(this.compressorRadius, this.decompressorRadius);
        int tallest = Math.max(this.generatorLayersBelow, this.generatorLayersAbove);
        return Math.max(Math.max(widest, this.generatorHalfWidth), tallest);
    }

    /** How many POWER ports this band's shell needs at minimum (more ports, not bigger numbers, is the FE story). */
    public int minPowerPorts() {
        return this.minPowerPorts;
    }

    /**
     * FE/tick demanded by this band's <em>first</em> generator rung; every rung above it doubles the bill.
     * PLACEHOLDER - the curve across bands is open decision D5, and FE is an {@code int} currency, so late bands must
     * scale through {@link #minPowerPorts()} rather than one enormous number.
     */
    public long baseEnergyPerTick() {
        return this.baseEnergyPerTick;
    }

    // --- Derived log10 BCE figures -------------------------------------------------------------

    /** log10 of the BCE mass of one block at this band's ceiling: {@code 9^T}. */
    public double maxOutputMassLog() {
        return CompressionConfig.bceMassLog(this.maxOutputTier);
    }

    /** Per-transfer ceiling of this band's pipe tier: one full ceiling block's mass. */
    public double pipeCeilingLog() {
        return maxOutputMassLog();
    }

    /** Nominal capacity of this band's storage tier: one full ceiling block's mass. */
    public double storageCapacityLog() {
        return maxOutputMassLog();
    }

    /**
     * The smallest buffer that can hold <em>one whole</em> band-max lump: {@link #maxOutputMassLog()} plus the
     * transfer headroom {@link CompressionConfig#withTransferHeadroom} exists to add. Generators use this instead of
     * {@link #storageCapacityLog()} because a mint now lands as a single lump whose size <em>is</em> the band ceiling,
     * and a pool cannot express "ceiling minus the last slice" once the two figures round to the same {@code double}.
     */
    public double lumpCeilingLog() {
        return CompressionConfig.withTransferHeadroom(maxOutputMassLog());
    }

    /**
     * A sink that must accept this band's biggest push needs a ceiling <em>above</em> that push or
     * {@link CompressionConfig#log10subtract} cannot express the last slice of room. Two guards, in order:
     * {@link CompressionConfig#withTransferHeadroom} adds a margin that survives the rounding at magnitudes where a
     * fixed additive headroom would vanish (MASTER_PLAN §4.5), and {@link CompressionConfig#UNLIMITED_SINK_FLOOR_LOG}
     * holds the result up to the old "effectively unlimited" figure so adopting the band table never <em>shrank</em> a
     * shipped machine's pool. Net effect: bands 0-1 behave exactly as they always did, band 2 onward grows with the
     * band instead of hitting a constant it would outgrow.
     */
    public double sinkCeilingLog() {
        return Math.max(CompressionConfig.UNLIMITED_SINK_FLOOR_LOG,
                CompressionConfig.withTransferHeadroom(maxOutputMassLog()));
    }

    /** Tier the band's generator ladder starts at: the previous band's ceiling (band 1 starts at 1000, as shipped). */
    public BigInteger rungStartExponent() {
        return minInputTier();
    }

    /**
     * Tier the band's top generator rung mints toward: <b>half</b> this band's ceiling, the geometric-half convention
     * the shipped ladders use (band 0's top rung is {@code 9^500}, half a tier-1000 block's BCE; band 1's is
     * {@code 9^(5*10^11)}, half of {@code 9^(10^12)}).
     *
     * <p>The other half is closed by the <em>array</em>, not by a bigger rung: one tower only ever mints
     * {@link #shareOf(BigInteger) share} of its rung, and {@link #arrayFullOutputTowers()} of them add up to one whole
     * band-max block per second. Keeping the rung at the geometric half is what makes tower <em>count</em> the
     * progression verb - see {@code MASTER_PLAN.md} D23.
     */
    public BigInteger rungEndExponent() {
        return this.maxOutputTier.shiftRight(1);
    }

    // --- Generator array (D23) -----------------------------------------------------------------

    /**
     * How far a tower of this band's share is shifted down from its rung's nominal exponent: the band index itself,
     * so band 0 is undivided (a tier-0 generator gives exactly what its rung says) and every band after it halves the
     * share again. This one number is the whole "higher band, more towers" curve.
     */
    public int arrayShift() {
        return this.index();
    }

    /**
     * What one tower of this band actually mints from a rung whose exponent is {@code rungExponent}: that exponent
     * divided by {@code 2^arrayShift()}. Done in {@link BigInteger} because the quotient is the thing being summed -
     * adding the two <em>masses</em> instead would land back in a {@code double} whose resolution at these magnitudes
     * is thousands to billions of decades wide, where {@code N} towers are indistinguishable from one.
     */
    public BigInteger shareOf(BigInteger rungExponent) {
        return rungExponent.shiftRight(arrayShift());
    }

    /**
     * How many top-rung towers one bus needs to mint a whole band-max block per second: {@code 2^(band + 1)} - 2 at
     * band 0, 4 at band 1, 8 at band 2, and 512 at band 8. Only ever used for readouts; the rule itself lives in
     * {@link #shareOf(BigInteger)} and {@link #rungEndExponent()}.
     */
    public long arrayFullOutputTowers() {
        return 2L << arrayShift();
    }

    // --- Table queries -------------------------------------------------------------------------

    /** Band by index, clamped so a bad config value degrades to a real band instead of throwing. */
    public static CompressionBand of(int index) {
        CompressionBand[] bands = values();
        return bands[Math.clamp(index, 0, bands.length - 1)];
    }

    /**
     * The widest host scan any band can demand - the {@link #shellExtent()} of the largest row. Callers that resolve a
     * port to its core cannot know which band built the shell they are standing in, so they scan this far, ordered
     * outward from the cheap centre (see {@code IMultiblockPortHost}).
     */
    public static int maxShellExtent() {
        int widest = 0;
        for (CompressionBand band : values()) {
            widest = Math.max(widest, band.shellExtent());
        }
        return widest;
    }

    /** The band whose ceiling first reaches or exceeds {@code tier} (used to pick a per-band display window). */
    public static CompressionBand bandOfTier(BigInteger tier) {
        for (CompressionBand band : values()) {
            if (tier.compareTo(band.maxOutputTier) <= 0) {
                return band;
            }
        }
        return TOP;
    }
}
