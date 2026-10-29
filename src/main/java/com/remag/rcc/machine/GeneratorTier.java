package com.remag.rcc.machine;

import com.remag.rcc.registry.BceNotation;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

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

    public CompressionBand band() {
        return CompressionBand.BAND_0;
    }

    public BigInteger shareExponent() {
        return band().shareOf(BigInteger.valueOf(this.tierExponent));
    }

    public double rateLog10PerSec() {
        return CompressionConfig.generatorRateLog10PerSec(this.tierExponent);
    }

    public boolean outputIsItems() {
        return rateLog10PerSec() <= Math.log10(CompressionConfig.GEN_ITEM_OUTPUT_LIMIT);
    }

    // Item rungs price upward from the tier-5 unlock milestone (Base=5, then one gate per rung); BCE rungs
    // gate at their own exponent, so the whole ladder stays monotonic (5,6,7,8,9,10,50,...).
    public int gateCobblestoneTier() {
        if (outputIsItems()) {
            return CompressionConfig.GEN_BASE_GATE_TIER + ordinal();
        }
        return this.tierExponent;
    }

    public String describe() {
        if (outputIsItems()) {
            return ratePerSecondText() + " cobblestone/sec";
        }
        return BceNotation.flat(rateTower()) + " BCE/sec";
    }

    @Nullable
    public BceNotation.Tower rateTower() {
        return outputIsItems() ? null : BceNotation.power("9", shareExponent());
    }

    public long ratePerSecond() {
        if (!outputIsItems()) {
            throw new IllegalStateException("BCE rung " + name + " has no int per-second count");
        }
        return tierExponent < 0
                ? CompressionConfig.GEN_BASE_COBBLES_PER_SEC
                : (long) Math.pow(9.0D, tierExponent);
    }

    public String ratePerSecondText() {
        return String.format(java.util.Locale.US, "%,d", ratePerSecond());
    }

    public String displaySuffix() {
        if (this == BASE) {
            return "Base";
        }
        if (outputIsItems()) {
            return DisplayNames.roman(ordinal());
        }
        return "BCE Tier " + tierExponent;
    }

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
