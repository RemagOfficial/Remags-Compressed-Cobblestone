package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.recipe.CompressRecipe;
import com.remag.rcc.recipe.DecompressRecipe;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ModRecipeProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(pOutput, lookupProvider);
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RCC.MODID, path);
    }

    @Override
    protected void buildRecipes(RecipeOutput pWriter) {
        // Level 1 is the only data-driven recipe: 3x3 vanilla cobblestone -> compressed(level 1).
        // All higher compressions/decompressions are handled by the two dynamic recipes in Phase 3,
        // so the recipe count no longer scales with MAX_LEVELS.
        ItemStack levelOne = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        levelOne.set(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, levelOne)
                .define('#', Blocks.COBBLESTONE)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .unlockedBy("has_cobblestone", has(Blocks.COBBLESTONE))
                .save(pWriter, rl("compressed_cobblestone_one"));

        // The two dynamic recipes are tier-agnostic (they read the level off the grid), so each is a
        // single JSON regardless of MAX_LEVELS. No unlock advancement is attached.
        pWriter.accept(rl("compression"), new CompressRecipe(), null);
        pWriter.accept(rl("decompression"), new DecompressRecipe(), null);
    }
}
