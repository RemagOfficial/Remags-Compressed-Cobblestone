package com.remag.rcc.machine;

import java.math.BigInteger;

/**
 * Implemented by machines whose GUI offers a selectable <em>goal tier</em> - the Auto Compressor and the Auto
 * Decompressor. Kept separate from {@link ICompressionMachine} so the shared
 * {@link com.remag.rcc.menu.CompressionCoreMenu} and {@link com.remag.rcc.client.CompressionCoreScreen} surface the
 * extra text-field control only when the host is such a machine, leaving the manipulator's GUI untouched.
 *
 * <p>The direction the seed travels toward the goal is machine-specific and lives in the marker sub-interfaces
 * {@link ICompressTarget} (climbs up to the goal) and {@link IDecompressTarget} (descends to the goal); this type only
 * carries the stored value and its (clamped) setter.
 */
public interface IGoalTierTarget {
    /** The tier the machine works its seed toward; it stops once the seed reaches this value. */
    BigInteger getGoalTier();

    /** Set the goal tier (the implementation is responsible for clamping it to a valid range). */
    void setGoalTier(BigInteger goalTier);

    /** The current goal tier as a plain decimal string, for prefilling the GUI text field. */
    String goalTierString();
}
