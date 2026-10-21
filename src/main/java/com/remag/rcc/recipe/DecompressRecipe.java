package com.remag.rcc.recipe;

import com.remag.rcc.item.ModItems;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.math.BigInteger;

/**
 * Dynamic decompression: a single compressed item at level {@code n} on its own crafts nine items one
 * tier down. Level 2+ gives nine of level {@code n-1}; level 1 gives nine vanilla cobblestone, mirroring
 * the static {@code 9 cobblestone -> level 1} recipe so every tier round-trips.
 *
 * <p>One recipe instance covers every tier. It is {@link RecipeType#CRAFTING} so any crafting grid works,
 * and requires exactly one slot holding exactly one compressed item, which keeps vanilla's consumption
 * exact (one in, nine out).
 *
 * <p>It implements {@link CraftingRecipe} (not just {@code Recipe}) because it is registered under
 * {@link RecipeType#CRAFTING}: consumers such as JEI cast every crafting-type recipe to
 * {@code CraftingRecipe}, so a bare {@code Recipe<CraftingInput>} would throw a {@code ClassCastException}.
 */
public class DecompressRecipe implements CraftingRecipe {

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack single = null;
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty()) {
                if (single != null) {
                    return false; // more than one slot occupied
                }
                single = stack;
            }
        }
        if (single == null || single.getCount() != 1) {
            return false;
        }
        return single.is(ModItems.COMPRESSED_COBBLESTONE.get());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        BigInteger tier = BigInteger.ONE;
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty()) {
                tier = stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
                break;
            }
        }
        ItemStack result = tier.equals(BigInteger.ONE)
                ? new ItemStack(Items.COBBLESTONE)
                : CompressRecipe.levelStack(tier.subtract(BigInteger.ONE));
        result.setCount(9);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        // The real output depends on the input tier; the recipe book only needs a representative stack.
        return new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.withSize(1, Ingredient.of(ModItems.COMPRESSED_COBBLESTONE.get()));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.DECOMPRESS.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
