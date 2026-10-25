package com.remag.rcc.registry;

import java.math.BigInteger;
import java.util.Locale;

/**
 * Rendering for numbers this mod can only express as <b>powers of powers</b>.
 *
 * <p>A tier-{@code t} block is worth {@code 9^t} cobblestone, so the figures on the tooltips and chat readouts are
 * exponential twice over: the tier itself is a band-1 number like {@code 10^12}, and the mass it exponentiates is
 * {@code 9^(10^12)}. Plain digits stop being an option around band 2, and the old single-level formatter had a hard
 * ceiling because of it: {@code (long) Math.floor(9.54e99)} saturates to {@link Long#MAX_VALUE}, so the deepest
 * milestone the mod is <em>aiming at</em> printed {@code 1.00 × 10^9223372036854775807} - a wrong number, not an
 * ugly one. Everything here is built so that no exponent is ever asked to fit a {@code long}.
 *
 * <p>The model is a {@link Tower}: a prefix plus one raised exponent level, recursively. Two renderings share it -
 * {@link #flat(Tower)} for text (Unicode superscripts while they still help, {@code ^(...)} nesting once they do
 * not), and the client-side {@code TowerTooltipComponent} for tooltips, which <em>draws</em> each level smaller and
 * higher so a stack reads as a real power tower.
 */
public final class BceNotation {
    /**
     * The largest exponent still spelled out as a plain integer. Above it the exponent gets its own power of ten,
     * because a six-digit superscript is unreadable and anything bigger is a lie anyway: a {@code double} only holds
     * integers exactly to {@code 2^53 ~ 9.0e15}, so the low digits of a longer one would be rounding noise. Six
     * digits covers every tier a shipped machine can reach today ({@code 9^1000} is {@code 10^954}).
     */
    public static final double FLAT_EXPONENT_LIMIT = 1.0E6D;
    /** Decimal places on a mantissa (e.g. {@code 9.54 × 10^99}). */
    public static final int MANTISSA_DECIMALS = 2;
    /** Unicode superscript digits, indexed 0..9 - the one level of superscripting that plain text can express. */
    private static final char[] SUPERSCRIPT = {
            '\u2070', '\u00b9', '\u00b2', '\u00b3', '\u2074', '\u2075', '\u2076', '\u2077', '\u2078', '\u2079'
    };

    private BceNotation() {
    }

    /**
     * One level of a power tower: either a {@link Plain} leaf or a {@link Level} prefix with a raised exponent.
     * Both expose the same two accessors so a renderer (text or drawn) can walk the chain without pattern matching.
     */
    public sealed interface Tower permits Tower.Plain, Tower.Level {
        /** Text drawn at this level's own size - for a {@link Level} this is e.g. {@code "9"} or {@code "9.54 × 10"}. */
        String prefix();

        /** The exponent sitting one level up, or {@code null} at the top of the tower. */
        Tower exponent();

        /** How many levels tall this tower is (a leaf is 1). */
        int depth();

        /** A finished string with no exponent - small values, or the leaf of a tower. */
        record Plain(String text) implements Tower {
            @Override
            public String prefix() {
                return text;
            }

            @Override
            public Tower exponent() {
                return null;
            }

            @Override
            public int depth() {
                return 1;
            }
        }

        /** {@code prefix^exponent}, drawn or nested one level up. */
        record Level(String prefix, Tower exponent) implements Tower {
            @Override
            public int depth() {
                return 1 + exponent.depth();
            }
        }
    }

    /**
     * The value {@code 10^log10Amount} as a tower. Below {@link #FLAT_EXPONENT_LIMIT} this is ordinary scientific
     * notation with an integer exponent; above it the exponent is itself written as a power of ten, which is the
     * stack - and the mantissa is dropped, because at that magnitude the fractional part of the log is rounding
     * noise rather than information.
     */
    public static Tower ofLog10(double log10Amount) {
        if (Double.isNaN(log10Amount) || log10Amount <= Double.NEGATIVE_INFINITY) {
            return new Tower.Plain("0");
        }
        if (log10Amount == Double.POSITIVE_INFINITY) {
            return new Tower.Plain("\u221e");
        }
        if (log10Amount <= 0.0D) {
            return new Tower.Plain("1");
        }
        if (log10Amount < FLAT_EXPONENT_LIMIT) {
            return scientific(log10Amount);
        }
        return new Tower.Level("10", scientific(Math.log10(log10Amount)));
    }

    /**
     * A whole number as a tower: spelled out while it is short, else a power of ten with a flat exponent (a
     * {@link BigInteger}'s digit count is an {@code int}, so this never needs a third level).
     */
    public static Tower ofBigInteger(BigInteger value) {
        if (value == null) {
            return new Tower.Plain("0");
        }
        double asLog = value.signum() > 0 ? CompressionConfig.log10(value) : 0.0D;
        if (asLog < Math.log10(FLAT_EXPONENT_LIMIT)) {
            return new Tower.Plain(String.format(Locale.US, "%,d", value));
        }
        return scientific(asLog);
    }

    /** {@code base^exponent} as a tower - the shape every generator rung's rate is written in. */
    public static Tower power(String base, BigInteger exponent) {
        if (exponent == null) {
            return new Tower.Plain(base);
        }
        double asLog = exponent.signum() > 0 ? CompressionConfig.log10(exponent) : 0.0D;
        if (asLog < Math.log10(FLAT_EXPONENT_LIMIT)) {
            return new Tower.Level(base, new Tower.Plain(exponent.toString()));
        }
        return new Tower.Level(base, scientific(asLog));
    }

    /**
     * The compact {@code 10^N} form of a log10 <em>figure</em> (a mass or a rate held as {@code log10(amount)}), used
     * by chat readouts and tank gauges: whole exponents stay exact, anything else shows one decimal, and a magnitude
     * too large to spell gets stacked rather than truncated.
     */
    public static Tower magnitude(double log10Amount) {
        if (Double.isNaN(log10Amount) || log10Amount <= 0.0D || log10Amount == Double.NEGATIVE_INFINITY) {
            return new Tower.Plain("0");
        }
        if (log10Amount == Double.POSITIVE_INFINITY) {
            return new Tower.Plain("\u221e");
        }
        if (log10Amount >= FLAT_EXPONENT_LIMIT) {
            return new Tower.Level("10", scientific(Math.log10(log10Amount)));
        }
        if (Math.abs(log10Amount - Math.rint(log10Amount)) < 1.0E-3D) {
            return new Tower.Level("10", new Tower.Plain(Long.toString(Math.round(log10Amount))));
        }
        return new Tower.Level("10", new Tower.Plain(String.format(Locale.US, "%.1f", log10Amount)));
    }

    /**
     * {@code 10^log10Amount} in textbook scientific form: a mantissa in {@code [1,10)} and an integer exponent, which
     * the caller guarantees is small enough to spell ({@code < }{@link #FLAT_EXPONENT_LIMIT}). A mantissa that rounds
     * up to {@code 10.00} folds into a bumped exponent, and an exact power of ten drops the redundant
     * {@code "1.00 \u00d7"}.
     */
    private static Tower scientific(double log10Amount) {
        long exponent = (long) Math.floor(log10Amount);
        double mantissa = Math.pow(10.0, log10Amount - exponent);
        String mantissaStr = String.format(Locale.US, "%." + MANTISSA_DECIMALS + "f", mantissa);
        if (mantissaStr.equals("10.00")) {
            mantissaStr = "1.00";
            exponent += 1;
        }
        if (mantissaStr.equals("1.00")) {
            return new Tower.Level("10", new Tower.Plain(Long.toString(exponent)));
        }
        return new Tower.Level(mantissaStr + " \u00d7 10", new Tower.Plain(Long.toString(exponent)));
    }

    /**
     * The tower as plain text. One level deep uses the Unicode superscripts the mod has always printed
     * ({@code 9\u2070\u00b2\u00b3}); deeper levels cannot be superscripted - Unicode has no superscript of a
     * superscript - so they nest with {@code ^(...)} instead, e.g. {@code 10^(9.54 \u00d7 10\u2079\u2079)}.
     */
    public static String flat(Tower tower) {
        if (tower == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        append(tower, sb);
        return sb.toString();
    }

    private static void append(Tower tower, StringBuilder sb) {
        sb.append(tower.prefix());
        Tower up = tower.exponent();
        if (up == null) {
            return;
        }
        if (up instanceof Tower.Plain plain) {
            // The common case: one exponent, so the superscript glyphs can carry it without any brackets.
            sb.append(superscript(plain.text().replace(",", "")));
            return;
        }
        sb.append("^(");
        append(up, sb);
        sb.append(')');
    }

    /** ASCII digits (and a decimal point, as a raised middle dot) as Unicode superscripts. */
    public static String superscript(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                sb.append('\u00b7'); // raised dot as the decimal separator inside an exponent
            } else if (c >= '0' && c <= '9') {
                sb.append(SUPERSCRIPT[c - '0']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Text form of {@code 10^log10Amount} (see {@link #ofLog10}). */
    public static String flatLog10(double log10Amount) {
        return flat(ofLog10(log10Amount));
    }

    /** Text form of a log10 figure (see {@link #magnitude}). */
    public static String flatMagnitude(double log10Amount) {
        return flat(magnitude(log10Amount));
    }
}
