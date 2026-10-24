package com.remag.rcc.machine;

/**
 * Marker for a {@link IGoalTierTarget} that runs the ladder <em>upward</em>: the Auto Compressor climbs its seed
 * toward the goal and stops (ejecting the finished block) once the seed reaches it. Defaults to the top tier
 * ({@link com.remag.rcc.registry.CompressionConfig#AUTO_TARGET_LEVEL}), so out of the box it still compresses all the
 * way to 1000; lowering the goal lets a player stop partway and uncraft back down by hand. The shared GUI uses this
 * type to pick the compressor's panel art; the goal value itself is carried by {@link IGoalTierTarget}.
 */
public interface ICompressTarget extends IGoalTierTarget {
}
