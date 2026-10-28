package com.remag.rcc.jei;

import net.minecraft.world.item.ItemStack;

public record ManipulatorDisplayRecipe(ItemStack seed, ItemStack output, int energyPerCraft, double requiredFuelLog) {
}
