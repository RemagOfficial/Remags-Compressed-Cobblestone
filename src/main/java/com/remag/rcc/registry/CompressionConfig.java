package com.remag.rcc.registry;

import java.math.BigInteger;

public final class CompressionConfig {
    public static final BigInteger MANUAL_MAX_LEVEL = CompressionBand.BAND_0.maxOutputTier();
    public static final BigInteger MANIPULATOR_MIN_IN = CompressionBand.BAND_1.minInputTier();
    public static final BigInteger MANIPULATOR_MAX_OUT = CompressionBand.BAND_1.maxOutputTier();

    public static final float TINT_BASE_GREY = 177f / 255f;
    public static final int[] TINT_ANCHOR_ARGB = {
            argbGrey(TINT_BASE_GREY),
            0xFF_000000,
            0xFF_FFFFFF,
            0xFF_FF0000,
            0xFF_FF8000,
            0xFF_FFFF00,
            0xFF_00FF00,
            0xFF_00FFFF,
            0xFF_0000FF,
            0xFF_8000FF
    };
    public static final int[] PIPE_TINT_ARGB = {
            0xFF_FFFFFF,
            0xFF_9FB4E0,
            0xFF_DCA0D2,
            0xFF_9FE0CE,
            0xFF_AEE09F,
            0xFF_E0D69F,
            0xFF_E0B39F,
            0xFF_E09F9F,
            0xFF_B69FE0
    };
    public static final double[] TINT_STOPS = buildTintStops();

    private static double[] buildTintStops() {
        CompressionBand[] bands = CompressionBand.values();
        double[] stops = new double[bands.length + 1];
        stops[0] = 0.0D;
        for (int i = 0; i < bands.length; i++) {
            stops[i + 1] = log10(bands[i].maxOutputTier());
        }
        return stops;
    }

    private static int argbGrey(float channel) {
        int v = Math.clamp(Math.round(255f * channel), 0, 255);
        return (0xFF << 24) | (v << 16) | (v << 8) | v;
    }

    public static final double LOG8 = Math.log10(8d);

    public static final double LOG9 = Math.log10(9d);

    public static final double BCE_TRANSFER_FLOOR_LOG = 10.0D;
    public static final double SINK_HEADROOM_RELATIVE = 1.0E-3D;
    public static final double UNLIMITED_SINK_FLOOR_LOG = 1.0E18D;

    public static final int REGION_RADIUS = CompressionBand.BAND_1.compressorRadius();
    public static final int ENERGY_CAPACITY = 1_000_000;
    public static final int ENERGY_PER_CRAFT = 250_000;
    public static final int TICKS_PER_CRAFT = 240;
    public static final double CRAFT_SLOWDOWN_PER_BAND = 1.0D;
    public static final int MIN_POWER_PORTS = CompressionBand.BAND_1.minPowerPorts();
    public static final int MIN_ITEM_IN_PORTS = 1;
    public static final int MIN_ITEM_OUT_PORTS = 1;
    public static final int MIN_INTERFACES = 1;
    public static final int MAX_INTERFACES = 1;
    public static final double MANIPULATOR_BCE_CAPACITY_LOG = CompressionBand.BAND_1.sinkCeilingLog();

    public static final BigInteger AUTO_TARGET_LEVEL = MANUAL_MAX_LEVEL;
    public static final int AUTO_TICKS_PER_STEP = 5;
    public static final int AUTO_ENERGY_CAPACITY = 100_000;
    public static final int AUTO_ENERGY_PER_STEP = 1_000;

    public static final double DECOMP_BCE_CAPACITY_LOG = CompressionBand.TOP.sinkCeilingLog();
    public static final double DECOMP_BCE_DISPLAY_LOG = bceMassLog(AUTO_TARGET_LEVEL);

    public static final int DECOMP_REGION_RADIUS = CompressionBand.BAND_1.decompressorRadius();
    public static final int DECOMP_MULTIBLOCK_TICKS_PER_STEP = 1;
    public static final int DECOMP_MULTIBLOCK_ENERGY_PER_STEP = AUTO_ENERGY_PER_STEP;

    public static final int GEN1_BASE_ENERGY_PER_TICK = (int) CompressionBand.BAND_1.baseEnergyPerTick();
    public static final int GEN1_REGION_RADIUS = CompressionBand.BAND_1.generatorHalfWidth();
    public static final int GEN1_LAYERS_BELOW = CompressionBand.BAND_1.generatorLayersBelow();
    public static final int GEN1_LAYERS_ABOVE = CompressionBand.BAND_1.generatorLayersAbove();
    public static final int MIN_GEN1_POWER_PORTS = CompressionBand.BAND_1.minPowerPorts();
    public static final int EXACT_GEN1_BCE_OUT_PORTS = 1;

    public static final double[] BCE_PIPE_MAX_TRANSFER_LOG =
            {CompressionBand.BAND_0.pipeCeilingLog(), CompressionBand.BAND_1.pipeCeilingLog(),
                    CompressionBand.BAND_2.pipeCeilingLog(), CompressionBand.BAND_3.pipeCeilingLog(),
                    CompressionBand.BAND_4.pipeCeilingLog(), CompressionBand.BAND_5.pipeCeilingLog(),
                    CompressionBand.BAND_6.pipeCeilingLog(), CompressionBand.BAND_7.pipeCeilingLog(),
                    CompressionBand.BAND_8.pipeCeilingLog()};
    public static final double BCE_STORAGE_CAPACITY_LOG = CompressionBand.BAND_0.storageCapacityLog();
    public static final double BCE_STORAGE_2_CAPACITY_LOG = CompressionBand.BAND_1.storageCapacityLog();
    public static final double BCE_STORAGE_3_CAPACITY_LOG = CompressionBand.BAND_2.storageCapacityLog();
    public static final double[] BCE_STORAGE_BAND_CAPACITY_LOGS =
            {CompressionBand.BAND_3.storageCapacityLog(), CompressionBand.BAND_4.storageCapacityLog(),
                    CompressionBand.BAND_5.storageCapacityLog(), CompressionBand.BAND_6.storageCapacityLog(),
                    CompressionBand.BAND_7.storageCapacityLog(), CompressionBand.BAND_8.storageCapacityLog()};
    public static final int BCE_NETWORK_ENDPOINT_TTL_TICKS = 100;
    public static final double AUTO_BCE_CAPACITY_LOG = withTransferHeadroom(BCE_STORAGE_CAPACITY_LOG);

    public static double withTransferHeadroom(double value) {
        if (value == Double.NEGATIVE_INFINITY) {
            return Double.NEGATIVE_INFINITY;
        }
        return Math.max(value + BCE_TRANSFER_FLOOR_LOG, value * (1.0D + SINK_HEADROOM_RELATIVE));
    }

    public static final int GEN_BASE_COBBLES_PER_SEC = 40;
    public static final int GEN_MAX_TIER_EXPONENT = 500;
    public static final int[] GEN_MILESTONE_EXPONENTS = {10, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500};
    public static final double GEN_ITEM_OUTPUT_LIMIT = 2147483647.0D;
    public static final int GEN_BASE_GATE_TIER = 5;
    public static double generatorRateLog10PerSec(int tierExponent) {
        return tierExponent < 0 ? Math.log10(GEN_BASE_COBBLES_PER_SEC) : bceMassLog(BigInteger.valueOf(tierExponent));
    }
    public static final int GEN_MINT_PERIOD_TICKS = 20;
    public static double generatorLumpCeilingLog(double lumpLog) {
        return withTransferHeadroom(lumpLog);
    }

    public static final float SEED_CUBE_SCALE = 1.2F;
    public static final float SEED_SPIN_SPEED = 4F;

    public static final boolean LIGHTNING_ENABLED = true;
    public static final int ZAP_INTERVAL_TICKS = 8;
    public static final int BOLTS_PER_ZAP = 8;
    public static final int BOLT_STEPS = 6;
    public static final float BOLT_JAG = 0.22F;
    public static final float BOLT_WIDTH = 0.02F;
    public static final int BOLT_TTL = 20;
    public static final int PULSE_BURST = 40;
    public static final float PULSE_START_RADIUS = 1F;
    public static final float PULSE_END_RADIUS = 1.4F;
    public static final float PULSE_LEN = 1F;
    public static final float PULSE_WIDTH = 0.04F;
    public static final int PULSE_TTL = 40;

    public static float seedCubeScale(CompressionBand band) {
        return SEED_CUBE_SCALE * Math.max(1, band.index());
    }

    public static float effectGrowth(CompressionBand band) {
        double chamber = Math.max(1, band.compressorRadius()) - 0.5D;
        double base = CompressionBand.BAND_1.compressorRadius() - 0.5D;
        return (float) (chamber / base);
    }

    public static float effectScale(CompressionBand band) {
        return Math.max(1.0F, (float) band.index());
    }

    public static float boltWidth(CompressionBand band) {
        return BOLT_WIDTH * effectScale(band);
    }

    public static float pulseWidth(CompressionBand band) {
        return PULSE_WIDTH * effectScale(band);
    }

    public static int boltsPerZap(CompressionBand band) {
        return Math.max(1, Math.round(BOLTS_PER_ZAP * (effectScale(band) / 4)));
    }

    public static int pulseBurst(CompressionBand band) {
        return Math.max(1, Math.round(PULSE_BURST * effectScale(band)));
    }

    public static int boltSteps(CompressionBand band) {
        return Math.max(1, Math.round(BOLT_STEPS * (float) Math.sqrt(effectScale(band))));
    }

    public static float boltJag(CompressionBand band) {
        return BOLT_JAG * effectScale(band);
    }

    public static int craftTicks(CompressionBand band) {
        return Math.max(1, (int) Math.round(TICKS_PER_CRAFT * Math.pow(CRAFT_SLOWDOWN_PER_BAND, band.index() - 1)));
    }

    private CompressionConfig() {
    }

    public static double log10(BigInteger value) {
        String s = value.toString();
        int head = Math.min(18, s.length());
        double leading = new BigInteger(s.substring(0, head)).doubleValue();
        return Math.log10(leading) + (s.length() - head);
    }

    private static int tintSegment(double exponent) {
        int last = Math.min(TINT_STOPS.length, TINT_ANCHOR_ARGB.length) - 2;
        for (int i = last; i >= 1; i--) {
            if (exponent > TINT_STOPS[i]) {
                return i;
            }
        }
        return 0;
    }

    public static int tintArgb(BigInteger level) {
        double exponent = log10(level.max(BigInteger.ONE));
        int segment = tintSegment(exponent);
        double span = TINT_STOPS[segment + 1] - TINT_STOPS[segment];
        float t = span <= 0.0D ? 1.0F : (float) Math.clamp((exponent - TINT_STOPS[segment]) / span, 0.0D, 1.0D);
        int from = TINT_ANCHOR_ARGB[segment];
        int to = TINT_ANCHOR_ARGB[segment + 1];
        int r = Math.round(((from >> 16) & 0xFF) + t * (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)));
        int g = Math.round(((from >> 8) & 0xFF) + t * (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)));
        int b = Math.round((from & 0xFF) + t * ((to & 0xFF) - (from & 0xFF)));
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    public static int bandTintArgb(CompressionBand band) {
        return TINT_ANCHOR_ARGB[band.index() + 1];
    }

    public static double tierMassLog(BigInteger tier) {
        return tier.subtract(BigInteger.ONE).doubleValue() * LOG9;
    }

    public static double requiredMassLog(BigInteger n) {
        return tierMassLog(n);
    }

    public static double bceMassLog(BigInteger tier) {
        return tier.doubleValue() * LOG9;
    }

    public static double requiredStepMassLog(BigInteger t) {
        return LOG8 + bceMassLog(t);
    }

    public static double log10subtract(double pool, double sub) {
        if (pool == Double.NEGATIVE_INFINITY) {
            return Double.NEGATIVE_INFINITY;
        }
        if (sub >= pool - 1.0E-9D) {
            return Double.NEGATIVE_INFINITY;
        }
        return pool + Math.log10(1.0 - Math.pow(10.0, sub - pool));
    }

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
