package com.remag.rcc.registry;

import java.math.BigInteger;

public final class DisplayNames {
    private static final String[] COMB = {
            "", "un", "duo", "tre", "quattuor", "quin", "sex", "septen", "octo", "novem"
    };
    private static final String[] STANDALONE = {
            "single", "double", "triple", "quadruple", "quintuple", "sextuple", "septuple", "octuple", "nonuple"
    };
    private static final String[] TENS = {
            "", "", "vigint", "trigint", "quadragint", "quinquagint", "sexagint", "septuagint", "octogint", "nonagint"
    };
    private static final String[] HUNDREDS = {
            "", "cent", "ducent", "trecent", "quadringent", "quingent", "sexcent", "septingent", "octingent", "nongent"
    };
    private static final int EXPONENT_DECIMALS = 3;
    private static final BigInteger MAX_EXACT_BCE_TIER = BigInteger.valueOf(19);

    private DisplayNames() {
    }

    public static String compressed(int level) {
        return capitalize(tupleWord(level)) + " Compressed Cobblestone";
    }

    public static String compressed(BigInteger level) {
        return capitalize(tupleWord(level)) + " Compressed Cobblestone";
    }

    static String tupleWord(BigInteger n) {
        if (n.compareTo(BigInteger.valueOf(1000)) < 0) {
            return tupleWord(n.intValue());
        }
        return powerOfTenWord(n);
    }

    public static String bceValueText(BigInteger tier) {
        if (tier.signum() <= 0) {
            return "1";
        }
        if (tier.compareTo(MAX_EXACT_BCE_TIER) <= 0) {
            long value = BigInteger.valueOf(9).pow(tier.intValue()).longValue();
            return String.format(java.util.Locale.US, "%,d", value);
        }
        return BceNotation.flatLog10(CompressionConfig.bceMassLog(tier));
    }

    public static BceNotation.Tower bceValueTower(BigInteger tier) {
        if (tier.signum() <= 0) {
            return new BceNotation.Tower.Plain("1");
        }
        if (tier.equals(CompressionBand.TOP.maxOutputTier())) {
            return new BceNotation.Tower.Level("10",
                    new BceNotation.Tower.Level("10", new BceNotation.Tower.Plain("100")));
        }
        if (tier.compareTo(MAX_EXACT_BCE_TIER) <= 0) {
            long value = BigInteger.valueOf(9).pow(tier.intValue()).longValue();
            return new BceNotation.Tower.Plain(String.format(java.util.Locale.US, "%,d", value));
        }
        return BceNotation.ofLog10(CompressionConfig.bceMassLog(tier));
    }

    public static String tupleWord(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("Compression levels start at 1, got: " + n);
        }
        if (n <= 9) {
            return STANDALONE[n - 1];
        }
        if (n >= 1000) {
            return powerOfTenWord(n);
        }
        int hundreds = n / 100;
        int tens = (n / 10) % 10;
        int ones = n % 10;

        String low = belowHundredWord(tens, ones);
        if (hundreds == 0) {
            return low + "uple";
        }
        String hundredRoot = HUNDREDS[hundreds];
        if (low.isEmpty()) {
            return hundredRoot + "uple";
        }
        return low + "a" + hundredRoot + "uple";
    }

    private static String belowHundredWord(int tens, int ones) {
        if (tens == 0) {
            return ones == 0 ? "" : COMB[ones];
        }
        if (tens == 1) {
            return COMB[ones] + "dec";
        }
        String root = TENS[tens];
        if (ones == 0) {
            return root;
        }
        return combine(COMB[ones], root) + root;
    }

    private static String combine(String prefix, String root) {
        boolean prefixEndsO = prefix.endsWith("o");
        boolean rootStartsVowel = !root.isEmpty() && isVowel(root.charAt(0));
        return (prefixEndsO && rootStartsVowel) ? prefix.substring(0, prefix.length() - 1) : prefix;
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    static String powerOfTenWord(BigInteger n) {
        String digits = n.toString();
        boolean exactPowerOfTen = digits.charAt(0) == '1' && digits.chars().skip(1).allMatch(c -> c == '0');
        if (exactPowerOfTen) {
            return "10" + superscript(Integer.toString(digits.length() - 1)) + "x";
        }
        double exponent = CompressionConfig.log10(n);
        String expStr = String.format(java.util.Locale.US, "%." + EXPONENT_DECIMALS + "f", exponent);
        return "10" + superscript(expStr) + "x";
    }

    static String powerOfTenWord(int n) {
        int digitExponent = Integer.toString(n).length() - 1;
        long pow = 1;
        for (int i = 0; i < digitExponent; i++) {
            pow *= 10;
        }
        if (n == pow) {
            return "10" + superscript(Integer.toString(digitExponent)) + "x";
        }
        double exponent = Math.log10(n);
        String expStr = String.format(java.util.Locale.US, "%." + EXPONENT_DECIMALS + "f", exponent);
        return "10" + superscript(expStr) + "x";
    }

    private static String superscript(String s) {
        return BceNotation.superscript(s);
    }

    public static String bceMagnitude(double log10Amount) {
        return BceNotation.flatMagnitude(log10Amount);
    }

    public static String roman(int number) {
        int[] values = {10, 9, 5, 4, 1};
        String[] symbols = {"X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        int remaining = Math.max(0, number);
        for (int i = 0; i < values.length && remaining > 0; i++) {
            while (remaining >= values[i]) {
                sb.append(symbols[i]);
                remaining -= values[i];
            }
        }
        return sb.toString();
    }

    // Band numeral for GUI/block titles: the first band (index 1) is un-numeraled to match its block name
    // ("Compression Core"), every later band gets its roman index (II, III, ...). Returns "" for band 1.
    public static String bandNumeral(CompressionBand band) {
        return band.index() < 2 ? "" : roman(band.index());
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
