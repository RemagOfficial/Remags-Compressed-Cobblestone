package com.remag.rcc.registry;

import java.math.BigInteger;
import java.util.Locale;

public final class BceNotation {
    public static final double FLAT_EXPONENT_LIMIT = 1.0E6D;
    public static final int MANTISSA_DECIMALS = 2;
    private static final char[] SUPERSCRIPT = {
            '\u2070', '\u00b9', '\u00b2', '\u00b3', '\u2074', '\u2075', '\u2076', '\u2077', '\u2078', '\u2079'
    };

    private BceNotation() {
    }

    public sealed interface Tower permits Tower.Plain, Tower.Level {
        String prefix();

        Tower exponent();

        int depth();

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

        record Level(String prefix, Tower exponent) implements Tower {
            @Override
            public int depth() {
                return 1 + exponent.depth();
            }
        }
    }

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
            sb.append(superscript(plain.text().replace(",", "")));
            return;
        }
        sb.append("^(");
        append(up, sb);
        sb.append(')');
    }

    public static String superscript(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                sb.append('\u00b7');
            } else if (c >= '0' && c <= '9') {
                sb.append(SUPERSCRIPT[c - '0']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String flatLog10(double log10Amount) {
        return flat(ofLog10(log10Amount));
    }

    public static String flatMagnitude(double log10Amount) {
        return flat(magnitude(log10Amount));
    }
}
