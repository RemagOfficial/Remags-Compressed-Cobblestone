package com.remag.rcc.registry;

import java.math.BigInteger;

/**
 * Human-readable "Nthuple" names for the creative tooltip / lang entries.
 *
 * <p>These are display strings only and do not gate the level count: the block/item/recipe/
 * texture pipeline is fully dynamic. Names are GENERATED from the level number using the classic
 * Latin multiplicative series (the same construction the previously hand-written table used:
 * units before tens, e.g. 21 = "un"+"vigint" -> "unvigintuple"; 101 = "un"+"cent" -> "uncentuple"),
 * so any {@link CompressionConfig#MANUAL_MAX_LEVEL} produces correct names with no table to maintain.
 *
 * <p>Compared to the old hand-typed table the stems are normalized to standard Latin
 * (40 "quadragintuple" not "tetragintuple", 100 "centuple" not "hectuple", etc.). Counts above 999
 * have no clean single-word form, so they are shown as powers of ten (e.g. 1000 = "10\u00b3x",
 * 1500 = "10\u00b3\u00b7\u00b9\u00f7\u2076x" with a fractional exponent).</p>
 */
public final class DisplayNames {
    // Combining unit prefixes, indexed by the digit 0..9 (index 0 is empty).
    private static final String[] COMB = {
            "", "un", "duo", "tre", "quattuor", "quin", "sex", "septen", "octo", "novem"
    };
    // Standalone words for levels 1..9 (irregular: "double", not "duo...").
    private static final String[] STANDALONE = {
            "single", "double", "triple", "quadruple", "quintuple", "sextuple", "septuple", "octuple", "nonuple"
    };
    // Tens roots, indexed by the tens digit (2..9 used; 1 is handled as the "-dec-" teens).
    private static final String[] TENS = {
            "", "", "vigint", "trigint", "quadragint", "quinquagint", "sexagint", "septuagint", "octogint", "nonagint"
    };
    // Hundreds roots, indexed by the hundreds digit (1..9).
    private static final String[] HUNDREDS = {
            "", "cent", "ducent", "trecent", "quadringent", "quingent", "sexcent", "septingent", "octingent", "nongent"
    };
    // Unicode superscript digits, indexed 0..9, used for power-of-ten exponents.
    private static final char[] SUPERSCRIPT = {
            '\u2070', '\u00b9', '\u00b2', '\u00b3', '\u2074', '\u2075', '\u2076', '\u2077', '\u2078', '\u2079'
    };
    // Decimal places shown for the fractional exponent of non-powers-of-ten (e.g. 1500 -> 10^3.176).
    // Raise it to distinguish levels that sit very close together just above a power of ten.
    private static final int EXPONENT_DECIMALS = 3;

    private DisplayNames() {
    }

    /** @param level 1-based compression level. */
    public static String compressed(int level) {
        return capitalize(tupleWord(level)) + " Compressed Cobblestone";
    }

    /**
     * @param level 1-based compression tier (arbitrarily large). Tiers up to 999 reuse the Latin multiplicative
     * word; anything larger is shown as a base-10 power, matching the {@code >= 1000} int path.
     */
    public static String compressed(BigInteger level) {
        return capitalize(tupleWord(level)) + " Compressed Cobblestone";
    }

    /** Latin word for small tiers, otherwise the power-of-ten rendering. */
    static String tupleWord(BigInteger n) {
        if (n.compareTo(BigInteger.valueOf(1000)) < 0) {
            return tupleWord(n.intValue());
        }
        return powerOfTenWord(n);
    }

    /** Builds the Latin multiplicative word for {@code n} (without the " Compressed Cobblestone" suffix). */
    static String tupleWord(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("Compression levels start at 1, got: " + n);
        }
        if (n <= 9) {
            return STANDALONE[n - 1];
        }
        if (n >= 1000) {
            // Past 999 there is no clean single-word Latin form, so show the level as a power of ten:
            // exact powers get an integer exponent, everything in between a fractional (decimal) one.
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
        // A connecting "a" always separates the remainder from the hundred root
        // (Latin "triginta centum" -> "trigintacent"). Keeping it for bare units too
        // (not just tens) avoids collisions such as 103 "trecent" == 300 "trecent".
        return low + "a" + hundredRoot + "uple";
    }

    /** Word for the 0..99 remainder, using the combining forms (empty string for 0). */
    private static String belowHundredWord(int tens, int ones) {
        if (tens == 0) {
            return ones == 0 ? "" : COMB[ones];
        }
        if (tens == 1) {
            // Teens: units (if any) prefix the "dec" root. 10 -> "dec", 11 -> "undec", 17 -> "septendec".
            return COMB[ones] + "dec";
        }
        String root = TENS[tens];
        if (ones == 0) {
            return root;
        }
        return combine(COMB[ones], root) + root;
    }

    /** Elides a trailing vowel from the unit prefix when the following root starts with a vowel
     * (e.g. "octo" + "octogint" -> "oct" + "octogint", "duo" + "octogint" -> "du" + "octogint"). */
    private static String combine(String prefix, String root) {
        boolean prefixEndsO = prefix.endsWith("o");
        boolean rootStartsVowel = !root.isEmpty() && isVowel(root.charAt(0));
        return (prefixEndsO && rootStartsVowel) ? prefix.substring(0, prefix.length() - 1) : prefix;
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    /**
     * Renders a large tier {@code n} (>= 1000) purely as a power of ten, suffixed with "x" to read as a
     * multiplier. Exact powers of ten show an integer exponent ({@code 1000 -> 10\u00b3x}); any value between
     * exact powers uses the fractional exponent {@code log\u2081\u2080(n)} at {@link #EXPONENT_DECIMALS} precision.
     */
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

    /**
     * Renders {@code n} (>= 1000) purely as a power of ten, suffixed with "x" to read as a multiplier.
     * Exact powers show an integer exponent ({@code 1000 -> 10\u00b3x}); any value between exact powers
     * uses the fractional exponent log\u2081\u2080(n) at {@link #EXPONENT_DECIMALS} precision
     * ({@code 1500 -> 10\u00b3\u00b7\u00b9\u2077\u2076x}), so the label always reflects the level's magnitude.
     */
    static String powerOfTenWord(int n) {
        int digitExponent = Integer.toString(n).length() - 1; // digit count - 1
        long pow = 1;
        for (int i = 0; i < digitExponent; i++) {
            pow *= 10;
        }
        if (n == pow) {
            // Exact power of ten -> clean integer exponent.
            return "10" + superscript(Integer.toString(digitExponent)) + "x";
        }
        // Between exact powers -> fractional exponent = log10(n), fixed decimal precision.
        double exponent = Math.log10(n);
        String expStr = String.format(java.util.Locale.US, "%." + EXPONENT_DECIMALS + "f", exponent);
        return "10" + superscript(expStr) + "x";
    }

    /** Converts ASCII digits (and the decimal point, to a superscript middle dot) to Unicode superscripts. */
    private static String superscript(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                sb.append('\u00b7'); // raised dot as the decimal separator inside the exponent
            } else if (c >= '0' && c <= '9') {
                sb.append(SUPERSCRIPT[c - '0']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
