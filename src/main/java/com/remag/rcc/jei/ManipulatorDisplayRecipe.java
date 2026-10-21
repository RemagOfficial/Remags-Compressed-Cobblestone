package com.remag.rcc.jei;

import net.minecraft.world.item.ItemStack;

/**
 * A display-only recipe for the Compression Manipulator's doubling cycle: one seed tier {@code n} in, the tier
 * {@code 2n} block out, alongside the fixed FE cost and the representative fuel mass (in log10-base-cobblestone
 * space) the machine must be fed before the cycle can run.
 *
 * <p>The real cycle is {@link com.remag.rcc.machine.CompressionCoreBlockEntity driven by the block entity} and
 * has no {@link net.minecraft.world.item.crafting.Recipe} of its own, so JEI can't discover it. The plugin pins a
 * few concrete seed tiers; the mass/energy figures come straight from {@link
 * com.remag.rcc.registry.CompressionConfig} so the illustration never drifts from the live machine.
 */
public record ManipulatorDisplayRecipe(ItemStack seed, ItemStack output, int energyPerCraft, double requiredFuelLog) {
}
