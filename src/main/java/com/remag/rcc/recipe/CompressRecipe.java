package com.remag.rcc.recipe;

import com.remag.rcc.item.ModItems;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.math.BigInteger;
import java.util.List;

public class CompressRecipe implements CraftingRecipe {

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }
        List<ItemStack> items = input.items();
        if (items.size() != 9) {
            return false;
        }
        BigInteger shared = null;
        for (ItemStack stack : items) {
            if (stack.isEmpty() || stack.getCount() != 1) {
                return false;
            }
            if (!stack.is(ModItems.COMPRESSED_COBBLESTONE.get())) {
                return false;
            }
            BigInteger tier = stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
            if (shared == null) {
                shared = tier;
            } else if (shared.compareTo(tier) != 0) {
                return false;
            }
        }
        return shared != null
                && shared.compareTo(BigInteger.ONE) >= 0
                && shared.compareTo(CompressionConfig.MANUAL_MAX_LEVEL) < 0;
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
        return levelStack(tier.add(BigInteger.ONE));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.withSize(9, Ingredient.of(ModItems.COMPRESSED_COBBLESTONE.get()));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.COMPRESS.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    static ItemStack levelStack(BigInteger tier) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), tier);
        return stack;
    }
}
