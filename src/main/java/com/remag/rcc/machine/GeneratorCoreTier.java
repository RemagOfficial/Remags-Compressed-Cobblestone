package com.remag.rcc.machine;

import com.remag.rcc.registry.BceNotation;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public final class GeneratorCoreTier {
    private final CompressionBand band;
    private final int index;
    private final BigInteger tierExponent;
    private final String name;

    public GeneratorCoreTier(CompressionBand band, int index) {
        List<BigInteger> rungs = band.rungExponents();
        int clamped = Math.min(Math.max(index, 0), Math.max(0, rungs.size() - 1));
        this.band = band;
        this.index = clamped;
        this.tierExponent = rungs.isEmpty() ? BigInteger.ZERO : rungs.get(clamped);
        this.name = computeName(band, clamped, this.tierExponent, rungs.size());
    }

    public static List<GeneratorCoreTier> rungsFor(CompressionBand band) {
        List<GeneratorCoreTier> rungs = new ArrayList<>();
        List<BigInteger> exponents = band.rungExponents();
        for (int i = 0; i < exponents.size(); i++) {
            rungs.add(new GeneratorCoreTier(band, i));
        }
        return rungs;
    }

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

    private static String shortName(BigInteger value) {
        int e = value.toString().length() - 1;
        if (e <= 3) {
            return BigInteger.TEN.pow(e).toString();
        }
        String[] suffixes = {"", "k", "m", "b", "t", "q", "x"};
        int group = e / 3;
        int within = e % 3;
        String mantissa = within == 0 ? "1" : BigInteger.TEN.pow(within).toString();
        String suffix = group < suffixes.length ? suffixes[group] : "e" + (3 * group);
        return mantissa + suffix;
    }

    public String getSerializedName() {
        return this.name;
    }

    public BigInteger tierExponent() {
        return this.tierExponent;
    }

    public CompressionBand band() {
        return this.band;
    }

    public int index() {
        return this.index;
    }

    public BigInteger shareExponent() {
        return this.band.shareOf(this.tierExponent);
    }

    public double rateLog10PerSec() {
        return CompressionConfig.bceMassLog(shareExponent());
    }

    private long rawEnergyPerTick() {
        long base = this.band.baseEnergyPerTick();
        int shifts = this.index;
        if (shifts >= Long.SIZE - 1 || base > (Long.MAX_VALUE >> shifts)) {
            return Long.MAX_VALUE;
        }
        return base << shifts;
    }

    public int energyPerTick() {
        return (int) Math.min(rawEnergyPerTick(), Integer.MAX_VALUE);
    }

    public int energyCapacity() {
        long perTick = rawEnergyPerTick();
        long capacity = perTick > Long.MAX_VALUE / 20 ? Long.MAX_VALUE : perTick * 20L;
        return (int) Math.min(capacity, Integer.MAX_VALUE);
    }

    public String describe() {
        return BceNotation.flat(rateTower()) + " BCE/sec";
    }

    public BceNotation.Tower rateTower() {
        return BceNotation.power("9", shareExponent());
    }

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
