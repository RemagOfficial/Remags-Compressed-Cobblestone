package com.remag.rcc.machine;

import com.remag.rcc.registry.BceNotation;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

/**
 * One rung of the cobblestone-generator ladder - the block-state value that decides a
 * {@link CobblestoneGeneratorBlock}'s output rate and mode. Follows the same "one block, an enum of variants"
 * pattern as {@link BcePipeTier}: a single registered block/item covers the whole ladder, and the placement writes
 * the tier into the state.
 *
 * <p>The ladder starts with {@link #BASE} - a fixed {@link CompressionConfig#GEN_BASE_COBBLES_PER_SEC} cobblestone/sec
 * hand-held drip - then every other rung rides the {@code 9^n} curve (a generator at exponent {@code n} makes the BCE
 * of one tier-{@code n} block every second). Rungs whose per-second output still fits an {@code int} (exponent up to
 * {@code 8}, since {@code 9^9} overflows) emit cobblestone <em>items</em>; from {@code 9^10} up the flow can only
 * travel as raw <em>BCE</em>. The mode is never stored - it falls out of {@link #outputIsItems()}, comparing the rate
 * against the int ceiling, so the ladder stays defined by its numbers alone.
 *
 * <p>The {@code TIER_*} rungs are the item-output band filling the gap between {@link #BASE} and the BCE transition;
 * the {@code BCE_TIER_*} rungs spell out the compression tier whose block-baseline ({@code 9^tier}) is their per-second
 * BCE rate, e.g. {@link #BCE_TIER_500} runs at one tier-500 block's BCE each second.
 *
 * <p>This ladder is band 0, so its array divisor is {@code 2^0 = 1}: a tier-0 generator mints exactly what its rung
 * names and was never divided. It still joins the {@code MASTER_PLAN.md} D23 array - two {@code 9^500} generators on
 * one BCE bus mint a whole tier-1000 block per second - which is where the rule starts and what makes the later bands
 * ask for 4, 8, ... towers. {@link GeneratorCoreTier} carries the full statement of the rule.
 */
public enum GeneratorTier implements StringRepresentable {
    BASE(-1, "base"),
    TIER_2(2, "tier_2"),
    TIER_4(4, "tier_4"),
    TIER_6(6, "tier_6"),
    TIER_8(8, "tier_8"),
    BCE_TIER_10(10, "bce_tier_10"),
    BCE_TIER_50(50, "bce_tier_50"),
    BCE_TIER_100(100, "bce_tier_100"),
    BCE_TIER_150(150, "bce_tier_150"),
    BCE_TIER_200(200, "bce_tier_200"),
    BCE_TIER_250(250, "bce_tier_250"),
    BCE_TIER_300(300, "bce_tier_300"),
    BCE_TIER_350(350, "bce_tier_350"),
    BCE_TIER_400(400, "bce_tier_400"),
    BCE_TIER_450(450, "bce_tier_450"),
    BCE_TIER_500(500, "bce_tier_500");

    /** The {@code 9^n} exponent ({@code 0}+ for the curve rungs), or {@code -1} for the fixed-rate {@link #BASE} drip. */
    private final int tierExponent;
    private final String name;

    GeneratorTier(int tierExponent, String name) {
        this.tierExponent = tierExponent;
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int tierExponent() {
        return this.tierExponent;
    }

    /** The band this ladder belongs to - the single blocks are band 0, whose array divisor is {@code 1}. */
    public CompressionBand band() {
        return CompressionBand.BAND_0;
    }

    /**
     * The exponent this generator contributes to its bus ({@link CompressionBand#shareOf}). Band 0 divides nothing, so
     * this equals {@link #tierExponent()}; it exists so the array has one rule for both generator families instead of a
     * special case for the single blocks. Only meaningful for a BCE rung - {@link #BASE} and the item rungs mint
     * physical cobblestone and are never array members.
     */
    public BigInteger shareExponent() {
        return band().shareOf(BigInteger.valueOf(this.tierExponent));
    }

    /** log10 of this rung's per-second output, in BCE (cobblestone-equivalents). */
    public double rateLog10PerSec() {
        return CompressionConfig.generatorRateLog10PerSec(this.tierExponent);
    }

    /**
     * True when this rung's per-second output still fits an {@code int} count, so it can be delivered as cobblestone
     * <em>items</em>; false once the flow is astronomical and can only travel as raw BCE. {@link #BASE} and every rung
     * at {@code 9^n} with {@code n <= 8} qualify ({@code 9^9 > Integer.MAX_VALUE}; see
     * {@link CompressionConfig#GEN_ITEM_OUTPUT_LIMIT}).
     *
     * <p>A BCE rung no longer has a per-tick rate or a rung-sized buffer ceiling: the bus mints one lump per
     * {@link CompressionConfig#GEN_MINT_PERIOD_TICKS} and sizes its own buffer - see
     * {@link CobblestoneGeneratorBlockEntity} and {@link CompressionConfig#generatorLumpCeilingLog(double)}.
     */
    public boolean outputIsItems() {
        return rateLog10PerSec() <= Math.log10(CompressionConfig.GEN_ITEM_OUTPUT_LIMIT);
    }

    /**
     * The compression tier of cobblestone that <em>gates</em> this rung on the progression line - the tier whose block
     * is worth the same as one second of this generator's output. For every curve/BCE rung that is exactly its own
     * exponent (a tier-{@code n} block is {@code 9^n} BCE, matching the rung's {@code 9^n}/sec); {@link #BASE} has no
     * aligned tier (its fixed {@link CompressionConfig#GEN_BASE_COBBLES_PER_SEC}/sec drip sits between two rungs) and
     * instead uses the hand-picked entry cost {@link CompressionConfig#GEN_BASE_GATE_TIER}. Drives both the advancement
     * trigger and the crafting recipe's centre ingredient, so the two always ask for the same block.
     */
    public int gateCobblestoneTier() {
        return this == BASE ? CompressionConfig.GEN_BASE_GATE_TIER : this.tierExponent;
    }

    /**
     * A compact, human-readable description of what this rung outputs, for the sneak-click chat readout. Item rungs
     * name their whole-cobblestone-per-second rate; BCE rungs name the compression tier whose block-baseline
     * ({@code 9^n}) is their per-second BCE rate, so {@link #BCE_TIER_500} reads {@code 9} with a superscript
     * {@code 500} followed by {@code BCE/sec}.
     */
    public String describe() {
        if (outputIsItems()) {
            return ratePerSecondText() + " cobblestone/sec";
        }
        return BceNotation.flat(rateTower()) + " BCE/sec";
    }

    /**
     * The rung's {@code 9^n} rate as a drawable power tower, or {@code null} for the rungs that still emit countable
     * items - those have no exponent to stack, only a whole-number per-second count. {@link #describe()} flattens the
     * same tower to text for chat, and {@link com.remag.rcc.item.CobblestoneGeneratorItem} hands it to the tooltip
     * graphic so the exponent is <em>raised</em> rather than written with a caret.
     */
    @Nullable
    public BceNotation.Tower rateTower() {
        return outputIsItems() ? null : BceNotation.power("9", shareExponent());
    }

    /**
     * The per-second output as a plain number for item rungs - {@link CompressionConfig#GEN_BASE_COBBLES_PER_SEC} for
     * {@link #BASE} and {@code 9^n} (exact, it fits an {@code int}) for a curve rung. Only meaningful while
     * {@link #outputIsItems()} holds; BCE rungs have no int-representable count.
     */
    public long ratePerSecond() {
        if (!outputIsItems()) {
            throw new IllegalStateException("BCE rung " + name + " has no int per-second count");
        }
        return tierExponent < 0
                ? CompressionConfig.GEN_BASE_COBBLES_PER_SEC
                : (long) Math.pow(9.0D, tierExponent);
    }

    /** {@link #ratePerSecond()} rendered with thousands separators, for names and tooltips. */
    public String ratePerSecondText() {
        return String.format(java.util.Locale.US, "%,d", ratePerSecond());
    }

    /**
     * The distinct per-rung name suffix the creative label appends to "Cobblestone Generator", so every rung reads
     * uniquely (e.g. {@code "Cobblestone Generator (Base)"}, {@code "Cobblestone Generator (double-tuple)"},
     * {@code "Cobblestone Generator (BCE Tier 500)"}). Uses the Latin {@code N-tuple} stem for the item curve rungs
     * (echoing the compressed-cobblestone naming) and a plain {@code BCE Tier N} for the BCE band.
     */
    public String displaySuffix() {
        if (this == BASE) {
            return "Base";
        }
        if (outputIsItems()) {
            return DisplayNames.tupleWord(tierExponent) + "-tuple";
        }
        return "BCE Tier " + tierExponent;
    }

    /** Lookup by the block-state serial name (used when reading a placed stack's {@code block_state} component). */
    @Nullable
    public static GeneratorTier byName(String name) {
        for (GeneratorTier tier : values()) {
            if (tier.name.equals(name)) {
                return tier;
            }
        }
        return null;
    }
}
