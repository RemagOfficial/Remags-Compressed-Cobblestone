package com.remag.rcc.registry;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public enum CompressionBand {
    BAND_0(BigInteger.TEN.pow(3), 0, 0, 0, 0, 0, 0, 0L),
    BAND_1(BigInteger.TEN.pow(15), 2, 1, 1, 1, 2, 1, 1_000L),
    BAND_2(BigInteger.TEN.pow(27), 3, 2, 2, 2, 3, 1, 2_000L),
    BAND_3(BigInteger.TEN.pow(39), 4, 3, 3, 3, 4, 2, 4_000L),
    BAND_4(BigInteger.TEN.pow(51), 5, 0, 4, 4, 5, 2, 8_000L),
    BAND_5(BigInteger.TEN.pow(63), 6, 0, 5, 5, 6, 2, 16_000L),
    BAND_6(BigInteger.TEN.pow(75), 7, 0, 6, 6, 7, 3, 32_000L),
    BAND_7(BigInteger.TEN.pow(87), 8, 0, 7, 7, 8, 3, 64_000L),
    BAND_8(BigInteger.TEN.pow(100), 9, 0, 8, 8, 9, 4, 128_000L);

    public static final CompressionBand TOP = values()[values().length - 1];

    public static final int RUNGS_PER_BAND = 13;

    private final BigInteger maxOutputTier;
    private final int compressorRadius;
    private final int decompressorRadius;
    private final int generatorHalfWidth;
    private final int generatorLayersBelow;
    private final int generatorLayersAbove;
    private final int recommendedPowerPorts;
    private final long baseEnergyPerTick;

    CompressionBand(BigInteger maxOutputTier, int compressorRadius, int decompressorRadius, int generatorHalfWidth,
                    int generatorLayersBelow, int generatorLayersAbove, int recommendedPowerPorts, long baseEnergyPerTick) {
        this.maxOutputTier = maxOutputTier;
        this.compressorRadius = compressorRadius;
        this.decompressorRadius = decompressorRadius;
        this.generatorHalfWidth = generatorHalfWidth;
        this.generatorLayersBelow = generatorLayersBelow;
        this.generatorLayersAbove = generatorLayersAbove;
        this.recommendedPowerPorts = recommendedPowerPorts;
        this.baseEnergyPerTick = baseEnergyPerTick;
    }

    public int index() {
        return this.ordinal();
    }

    public BigInteger maxOutputTier() {
        return this.maxOutputTier;
    }

    public BigInteger minInputTier() {
        return this.index() == 0 ? BigInteger.ZERO : values()[this.index() - 1].maxOutputTier();
    }

    public boolean hasStructure() {
        return this.compressorRadius > 0;
    }

    public int compressorRadius() {
        return this.compressorRadius;
    }

    public int decompressorRadius() {
        return this.decompressorRadius;
    }

    public boolean hasDecompressor() {
        return this.decompressorRadius > 0;
    }

    public int generatorHalfWidth() {
        return this.generatorHalfWidth;
    }

    public int generatorLayersBelow() {
        return this.generatorLayersBelow;
    }

    public int generatorLayersAbove() {
        return this.generatorLayersAbove;
    }

    public int generatorHeight() {
        return this.generatorLayersBelow + 1 + this.generatorLayersAbove;
    }

    public int shellExtent() {
        int widest = Math.max(this.compressorRadius, this.decompressorRadius);
        int tallest = Math.max(this.generatorLayersBelow, this.generatorLayersAbove);
        return Math.max(Math.max(widest, this.generatorHalfWidth), tallest);
    }

    public int minPowerPorts() {
        return this.hasStructure() ? 1 : 0;
    }

    public int recommendedPowerPorts() {
        return this.recommendedPowerPorts;
    }

    public long baseEnergyPerTick() {
        return this.baseEnergyPerTick;
    }

    public double maxOutputMassLog() {
        return CompressionConfig.bceMassLog(this.maxOutputTier);
    }

    public double pipeCeilingLog() {
        return maxOutputMassLog();
    }

    public double storageCapacityLog() {
        return maxOutputMassLog();
    }

    public double lumpCeilingLog() {
        return CompressionConfig.withTransferHeadroom(maxOutputMassLog());
    }

    public double sinkCeilingLog() {
        return Math.max(CompressionConfig.UNLIMITED_SINK_FLOOR_LOG,
                CompressionConfig.withTransferHeadroom(maxOutputMassLog()));
    }

    public BigInteger rungStartExponent() {
        return minInputTier();
    }

    public BigInteger rungEndExponent() {
        return this.maxOutputTier.shiftRight(1);
    }

    public List<BigInteger> rungExponents() {
        if (!this.hasStructure()) {
            return List.of();
        }
        BigInteger start = rungStartExponent();
        BigInteger end = rungEndExponent();
        int gaps = RUNGS_PER_BAND - 1;
        double startLog = log10Value(start);
        double span = log10Value(end) - startLog;
        List<BigInteger> rungs = new ArrayList<>(RUNGS_PER_BAND);
        rungs.add(start);
        for (int i = 1; i < gaps; i++) {
            int exponent = (int) Math.round(startLog + span * i / gaps);
            BigInteger rung = BigInteger.TEN.pow(Math.max(exponent, 1));
            while (rung.compareTo(rungs.get(rungs.size() - 1)) <= 0) {
                rung = rung.multiply(BigInteger.TEN);
            }
            if (rung.compareTo(end) >= 0) {
                break;
            }
            rungs.add(rung);
        }
        rungs.add(end);
        return rungs;
    }

    private static double log10Value(BigInteger value) {
        return Math.log10(value.doubleValue());
    }

    public static int maxRungCount() {
        int most = 0;
        for (CompressionBand band : values()) {
            most = Math.max(most, band.rungExponents().size());
        }
        return most;
    }

    public int arrayShift() {
        return this.index();
    }

    public BigInteger shareOf(BigInteger rungExponent) {
        return rungExponent.shiftRight(arrayShift());
    }

    public long arrayFullOutputTowers() {
        return 2L << arrayShift();
    }

    public static CompressionBand of(int index) {
        CompressionBand[] bands = values();
        return bands[Math.clamp(index, 0, bands.length - 1)];
    }

    public static int maxShellExtent() {
        int widest = 0;
        for (CompressionBand band : values()) {
            widest = Math.max(widest, band.shellExtent());
        }
        return widest;
    }

    public static CompressionBand bandOfTier(BigInteger tier) {
        for (CompressionBand band : values()) {
            if (tier.compareTo(band.maxOutputTier) <= 0) {
                return band;
            }
        }
        return TOP;
    }
}
