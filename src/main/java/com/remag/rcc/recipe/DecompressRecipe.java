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
                    return false;
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
