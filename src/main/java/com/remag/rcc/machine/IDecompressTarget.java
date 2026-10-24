package com.remag.rcc.machine;

/**
 * Marker for a {@link IGoalTierTarget} that runs the ladder <em>downward</em>: the Auto Decompressor descends its
 * seed toward the goal and stops once the seed reaches it. The shared GUI uses this type to pick the decompressor's
 * panel art; the goal value itself is carried by {@link IGoalTierTarget}.
 */
public interface IDecompressTarget extends IGoalTierTarget {
}
