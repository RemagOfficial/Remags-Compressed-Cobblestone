package com.remag.rcc.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A lightweight, display-only recipe used solely to show the compression mechanic in JEI.
 *
 * <p>The real {@link com.remag.rcc.recipe.CompressRecipe compress} / {@link com.remag.rcc.recipe.DecompressRecipe
 * decompress} recipes are tier-agnostic (they read the level off the crafting grid at runtime), so they carry no
 * concrete tier and can't be rendered as-is. These instances pin a tier for illustration; the plugin shows a few
 * consecutive examples and the mechanic generalises to every tier.
 */
public record CompressionDisplayRecipe(List<ItemStack> inputs, ItemStack output) {
}
