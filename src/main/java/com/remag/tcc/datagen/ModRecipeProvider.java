package com.remag.tcc.datagen;

import com.remag.tcc.block.ModBlocks;
import com.remag.tcc.item.ModItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {

        // Compressed Cobblestone Recipes

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.COBBLESTONE_ONE.get())
                .define('#', Blocks.COBBLESTONE)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .unlockedBy("has", has(ModItems.COBBLESTONE_ONE.get()))
                .save(pWriter, "compressed_cobblestone_one");

        Item[] items = {ModItems.COBBLESTONE_ONE.get(), ModItems.COBBLESTONE_TWO.get(), ModItems.COBBLESTONE_THREE.get(), ModItems.COBBLESTONE_FOUR.get(), ModItems.COBBLESTONE_FIVE.get(), ModItems.COBBLESTONE_SIX.get(), ModItems.COBBLESTONE_SEVEN.get(), ModItems.COBBLESTONE_EIGHT.get(), ModItems.COBBLESTONE_NINE.get(), ModItems.COBBLESTONE_TEN.get(), ModItems.COBBLESTONE_ELEVEN.get(), ModItems.COBBLESTONE_TWELVE.get(), ModItems.COBBLESTONE_THIRTEEN.get(), ModItems.COBBLESTONE_FOURTEEN.get(), ModItems.COBBLESTONE_FIFTEEN.get(), ModItems.COBBLESTONE_SIXTEEN.get(), ModItems.COBBLESTONE_SEVENTEEN.get(), ModItems.COBBLESTONE_EIGHTEEN.get(), ModItems.COBBLESTONE_NINETEEN.get(), ModItems.COBBLESTONE_TWENTY.get(), ModItems.COBBLESTONE_TWENTYONE.get(), ModItems.COBBLESTONE_TWENTYTWO.get(), ModItems.COBBLESTONE_TWENTYTHREE.get()};

        Block[] blocks = {ModBlocks.COBBLESTONE_ONE.get(), ModBlocks.COBBLESTONE_TWO.get(), ModBlocks.COBBLESTONE_THREE.get(), ModBlocks.COBBLESTONE_FOUR.get(), ModBlocks.COBBLESTONE_FIVE.get(), ModBlocks.COBBLESTONE_SIX.get(), ModBlocks.COBBLESTONE_SEVEN.get(), ModBlocks.COBBLESTONE_EIGHT.get(), ModBlocks.COBBLESTONE_NINE.get(), ModBlocks.COBBLESTONE_TEN.get(), ModBlocks.COBBLESTONE_ELEVEN.get(), ModBlocks.COBBLESTONE_TWELVE.get(), ModBlocks.COBBLESTONE_THIRTEEN.get(), ModBlocks.COBBLESTONE_FOURTEEN.get(), ModBlocks.COBBLESTONE_FIFTEEN.get(), ModBlocks.COBBLESTONE_SIXTEEN.get(), ModBlocks.COBBLESTONE_SEVENTEEN.get(), ModBlocks.COBBLESTONE_EIGHTEEN.get(), ModBlocks.COBBLESTONE_NINETEEN.get(), ModBlocks.COBBLESTONE_TWENTY.get(), ModBlocks.COBBLESTONE_TWENTYONE.get(), ModBlocks.COBBLESTONE_TWENTYTWO.get(), ModBlocks.COBBLESTONE_TWENTYTHREE.get()};

        // an array of spelt numbers from 1 to 23
        String[] speltNumbers = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty", "twentyone", "twentytwo", "twentythree"};

        for (int i = 0; i < items.length - 1; i++) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, blocks[i + 1])
                    .group("compress")
                    .define('#', items[i])
                    .pattern("###")
                    .pattern("###")
                    .pattern("###")
                    .unlockedBy(getHasName(items[i + 1]), has(items[i + 1]))
                    .save(pWriter, "compressed_cobblestone_" + speltNumbers[i + 1]);
        }

        // decompress recipes for cobblestone

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Blocks.COBBLESTONE, 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_ONE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_ONE.get()), has(ModItems.COBBLESTONE_ONE.get()))
                .save(pWriter, "decompressed_cobblestone_one");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_ONE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TWO.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWO.get()), has(ModItems.COBBLESTONE_TWO.get()))
                .save(pWriter, "decompressed_cobblestone_two");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWO.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_THREE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THREE.get()), has(ModItems.COBBLESTONE_THREE.get()))
                .save(pWriter, "decompressed_cobblestone_three");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THREE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FOUR.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FOUR.get()), has(ModItems.COBBLESTONE_FOUR.get()))
                .save(pWriter, "decompressed_cobblestone_four");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FOUR.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FIVE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIVE.get()), has(ModItems.COBBLESTONE_FIVE.get()))
                .save(pWriter, "decompressed_cobblestone_five");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIVE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SIX.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIX.get()), has(ModItems.COBBLESTONE_SIX.get()))
                .save(pWriter,  "decompressed_cobblestone_six");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIX.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SEVEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVEN.get()), has(ModItems.COBBLESTONE_SEVEN.get()))
                .save(pWriter, "decompressed_cobblestone_seven");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_EIGHT.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHT.get()), has(ModItems.COBBLESTONE_EIGHT.get()))
                .save(pWriter, "decompressed_cobblestone_eight");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHT.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_NINE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINE.get()), has(ModItems.COBBLESTONE_NINE.get()))
                .save(pWriter, "decompressed_cobblestone_nine");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TEN.get()), has(ModItems.COBBLESTONE_TEN.get()))
                .save(pWriter, "decompressed_cobblestone_ten");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_ELEVEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_ELEVEN.get()), has(ModItems.COBBLESTONE_ELEVEN.get()))
                .save(pWriter, "decompressed_cobblestone_eleven");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_ELEVEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TWELVE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWELVE.get()), has(ModItems.COBBLESTONE_TWELVE.get()))
                .save(pWriter, "decompressed_cobblestone_twelve");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWELVE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_THIRTEEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THIRTEEN.get()), has(ModItems.COBBLESTONE_THIRTEEN.get()))
                .save(pWriter, "decompressed_cobblestone_thirteen");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THIRTEEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FOURTEEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FOURTEEN.get()), has(ModItems.COBBLESTONE_FOURTEEN.get()))
                .save(pWriter, "decompressed_cobblestone_fourteen");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FOURTEEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FIFTEEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIFTEEN.get()), has(ModItems.COBBLESTONE_FIFTEEN.get()))
                .save(pWriter, "decompressed_cobblestone_fifteen");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIFTEEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SIXTEEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIXTEEN.get()), has(ModItems.COBBLESTONE_SIXTEEN.get()))
                .save(pWriter, "decompressed_cobblestone_sixteen");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIXTEEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SEVENTEEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVENTEEN.get()), has(ModItems.COBBLESTONE_SEVENTEEN.get()))
                .save(pWriter, "decompressed_cobblestone_seventeen");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVENTEEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_EIGHTEEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHTEEN.get()), has(ModItems.COBBLESTONE_EIGHTEEN.get()))
                .save(pWriter, "decompressed_cobblestone_eighteen");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHTEEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_NINETEEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINETEEN.get()), has(ModItems.COBBLESTONE_NINETEEN.get()))
                .save(pWriter, "decompressed_cobblestone_nineteen");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINETEEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TWENTY.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTY.get()), has(ModItems.COBBLESTONE_TWENTY.get()))
                .save(pWriter, "decompressed_cobblestone_twenty");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTY.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TWENTYONE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTYONE.get()), has(ModItems.COBBLESTONE_TWENTYONE.get()))
                .save(pWriter, "decompressed_cobblestone_twentyone");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTYONE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TWENTYTWO.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTYTWO.get()), has(ModItems.COBBLESTONE_TWENTYTWO.get()))
                .save(pWriter, "decompressed_cobblestone_twentytwo");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTYTWO.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TWENTYTHREE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTYTHREE.get()), has(ModItems.COBBLESTONE_TWENTYTHREE.get()))
                .save(pWriter, "decompressed_cobblestone_twentythree");
    }

}
