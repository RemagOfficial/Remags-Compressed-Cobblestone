package com.remag.rcc.datagen;

import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.item.ModItems;
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

        Item[] items = {ModItems.COBBLESTONE_ONE.get(), ModItems.COBBLESTONE_TWO.get(), ModItems.COBBLESTONE_THREE.get(), ModItems.COBBLESTONE_FOUR.get(), ModItems.COBBLESTONE_FIVE.get(), ModItems.COBBLESTONE_SIX.get(), ModItems.COBBLESTONE_SEVEN.get(), ModItems.COBBLESTONE_EIGHT.get(), ModItems.COBBLESTONE_NINE.get(), ModItems.COBBLESTONE_TEN.get(), ModItems.COBBLESTONE_ELEVEN.get(), ModItems.COBBLESTONE_TWELVE.get(), ModItems.COBBLESTONE_THIRTEEN.get(), ModItems.COBBLESTONE_FOURTEEN.get(), ModItems.COBBLESTONE_FIFTEEN.get(), ModItems.COBBLESTONE_SIXTEEN.get(), ModItems.COBBLESTONE_SEVENTEEN.get(), ModItems.COBBLESTONE_EIGHTEEN.get(), ModItems.COBBLESTONE_NINETEEN.get(), ModItems.COBBLESTONE_TWENTY.get(), ModItems.COBBLESTONE_TWENTYONE.get(), ModItems.COBBLESTONE_TWENTYTWO.get(), ModItems.COBBLESTONE_TWENTYTHREE.get(), ModItems.COBBLESTONE_TWENTYFOUR.get(), ModItems.COBBLESTONE_TWENTYFIVE.get(), ModItems.COBBLESTONE_TWENTYSIX.get(), ModItems.COBBLESTONE_TWENTYSEVEN.get(), ModItems.COBBLESTONE_TWENTYEIGHT.get(), ModItems.COBBLESTONE_TWENTYNINE.get(), ModItems.COBBLESTONE_THIRTY.get(), ModItems.COBBLESTONE_THIRTYONE.get(), ModItems.COBBLESTONE_THIRTYTWO.get(), ModItems.COBBLESTONE_THIRTYTHREE.get(), ModItems.COBBLESTONE_THIRTYFOUR.get(), ModItems.COBBLESTONE_THIRTYFIVE.get(), ModItems.COBBLESTONE_THIRTYSIX.get(), ModItems.COBBLESTONE_THIRTYSEVEN.get(), ModItems.COBBLESTONE_THIRTYEIGHT.get(), ModItems.COBBLESTONE_THIRTYNINE.get(), ModItems.COBBLESTONE_FORTY.get(), ModItems.COBBLESTONE_FORTYONE.get(), ModItems.COBBLESTONE_FORTYTWO.get(), ModItems.COBBLESTONE_FORTYTHREE.get(), ModItems.COBBLESTONE_FORTYFOUR.get(), ModItems.COBBLESTONE_FORTYFIVE.get(), ModItems.COBBLESTONE_FORTYSIX.get(), ModItems.COBBLESTONE_FORTYSEVEN.get(), ModItems.COBBLESTONE_FORTYEIGHT.get(), ModItems.COBBLESTONE_FORTYNINE.get(), ModItems.COBBLESTONE_FIFTY.get()};

        Block[] blocks = {ModBlocks.COBBLESTONE_ONE.get(), ModBlocks.COBBLESTONE_TWO.get(), ModBlocks.COBBLESTONE_THREE.get(), ModBlocks.COBBLESTONE_FOUR.get(), ModBlocks.COBBLESTONE_FIVE.get(), ModBlocks.COBBLESTONE_SIX.get(), ModBlocks.COBBLESTONE_SEVEN.get(), ModBlocks.COBBLESTONE_EIGHT.get(), ModBlocks.COBBLESTONE_NINE.get(), ModBlocks.COBBLESTONE_TEN.get(), ModBlocks.COBBLESTONE_ELEVEN.get(), ModBlocks.COBBLESTONE_TWELVE.get(), ModBlocks.COBBLESTONE_THIRTEEN.get(), ModBlocks.COBBLESTONE_FOURTEEN.get(), ModBlocks.COBBLESTONE_FIFTEEN.get(), ModBlocks.COBBLESTONE_SIXTEEN.get(), ModBlocks.COBBLESTONE_SEVENTEEN.get(), ModBlocks.COBBLESTONE_EIGHTEEN.get(), ModBlocks.COBBLESTONE_NINETEEN.get(), ModBlocks.COBBLESTONE_TWENTY.get(), ModBlocks.COBBLESTONE_TWENTYONE.get(), ModBlocks.COBBLESTONE_TWENTYTWO.get(), ModBlocks.COBBLESTONE_TWENTYTHREE.get(), ModBlocks.COBBLESTONE_TWENTYFOUR.get(), ModBlocks.COBBLESTONE_TWENTYFIVE.get(), ModBlocks.COBBLESTONE_TWENTYSIX.get(), ModBlocks.COBBLESTONE_TWENTYSEVEN.get(), ModBlocks.COBBLESTONE_TWENTYEIGHT.get(), ModBlocks.COBBLESTONE_TWENTYNINE.get(), ModBlocks.COBBLESTONE_THIRTY.get(), ModBlocks.COBBLESTONE_THIRTYONE.get(), ModBlocks.COBBLESTONE_THIRTYTWO.get(), ModBlocks.COBBLESTONE_THIRTYTHREE.get(), ModBlocks.COBBLESTONE_THIRTYFOUR.get(), ModBlocks.COBBLESTONE_THIRTYFIVE.get(), ModBlocks.COBBLESTONE_THIRTYSIX.get(), ModBlocks.COBBLESTONE_THIRTYSEVEN.get(), ModBlocks.COBBLESTONE_THIRTYEIGHT.get(), ModBlocks.COBBLESTONE_THIRTYNINE.get(), ModBlocks.COBBLESTONE_FORTY.get(), ModBlocks.COBBLESTONE_FORTYONE.get(), ModBlocks.COBBLESTONE_FORTYTWO.get(), ModBlocks.COBBLESTONE_FORTYTHREE.get(), ModBlocks.COBBLESTONE_FORTYFOUR.get(), ModBlocks.COBBLESTONE_FORTYFIVE.get(), ModBlocks.COBBLESTONE_FORTYSIX.get(), ModBlocks.COBBLESTONE_FORTYSEVEN.get(), ModBlocks.COBBLESTONE_FORTYEIGHT.get(), ModBlocks.COBBLESTONE_FORTYNINE.get(), ModBlocks.COBBLESTONE_FIFTY.get()};

        // an array of spelt numbers from 1 to 50
        String[] speltNumbers = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty", "twentyone", "twentytwo", "twentythree", "twentyfour", "twentyfive", "twentysix", "twentyseven", "twentyeight", "twentynine", "thirty", "thirtyone", "thirtytwo", "thirtythree", "thirtyfour", "thirtyfive", "thirtysix", "thirtyseven", "thirtyeight", "thirtynine", "forty", "fortyone", "fortytwo", "fortythree", "fortyfour", "fortyfive", "fortysix", "fortyseven", "fortyeight", "fortynine", "fifty"};

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

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTYTHREE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TWENTYFOUR.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTYFOUR.get()), has(ModItems.COBBLESTONE_TWENTYFOUR.get()))
                .save(pWriter, "decompressed_cobblestone_twentyfour");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTYFOUR.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TWENTYFIVE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTYFIVE.get()), has(ModItems.COBBLESTONE_TWENTYFIVE.get()))
                .save(pWriter, "decompressed_cobblestone_twentyfive");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTYFIVE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TWENTYSIX.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTYSIX.get()), has(ModItems.COBBLESTONE_TWENTYSIX.get()))
                .save(pWriter, "decompressed_cobblestone_twentysix");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTYSIX.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TWENTYSEVEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTYSEVEN.get()), has(ModItems.COBBLESTONE_TWENTYSEVEN.get()))
                .save(pWriter, "decompressed_cobblestone_twentyseven");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTYSEVEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TWENTYEIGHT.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTYEIGHT.get()), has(ModItems.COBBLESTONE_TWENTYEIGHT.get()))
                .save(pWriter, "decompressed_cobblestone_twentyeight");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTYEIGHT.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_TWENTYNINE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTYNINE.get()), has(ModItems.COBBLESTONE_TWENTYNINE.get()))
                .save(pWriter, "decompressed_cobblestone_twentynine");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTYNINE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_THIRTY.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THIRTY.get()), has(ModItems.COBBLESTONE_THIRTY.get()))
                .save(pWriter, "decompressed_cobblestone_thirty");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THIRTY.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_THIRTYONE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THIRTYONE.get()), has(ModItems.COBBLESTONE_THIRTYONE.get()))
                .save(pWriter, "decompressed_cobblestone_thirtyone");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THIRTYONE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_THIRTYTWO.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THIRTYTWO.get()), has(ModItems.COBBLESTONE_THIRTYTWO.get()))
                .save(pWriter, "decompressed_cobblestone_thirtytwo");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THIRTYTWO.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_THIRTYTHREE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THIRTYTHREE.get()), has(ModItems.COBBLESTONE_THIRTYTHREE.get()))
                .save(pWriter, "decompressed_cobblestone_thirtythree");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THIRTYTHREE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_THIRTYFOUR.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THIRTYFOUR.get()), has(ModItems.COBBLESTONE_THIRTYFOUR.get()))
                .save(pWriter, "decompressed_cobblestone_thirtyfour");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THIRTYFOUR.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_THIRTYFIVE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THIRTYFIVE.get()), has(ModItems.COBBLESTONE_THIRTYFIVE.get()))
                .save(pWriter, "decompressed_cobblestone_thirtyfive");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THIRTYFIVE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_THIRTYSIX.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THIRTYSIX.get()), has(ModItems.COBBLESTONE_THIRTYSIX.get()))
                .save(pWriter, "decompressed_cobblestone_thirtysix");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THIRTYSIX.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_THIRTYSEVEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THIRTYSEVEN.get()), has(ModItems.COBBLESTONE_THIRTYSEVEN.get()))
                .save(pWriter, "decompressed_cobblestone_thirtyseven");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THIRTYSEVEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_THIRTYEIGHT.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THIRTYEIGHT.get()), has(ModItems.COBBLESTONE_THIRTYEIGHT.get()))
                .save(pWriter, "decompressed_cobblestone_thirtyeight");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THIRTYEIGHT.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_THIRTYNINE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THIRTYNINE.get()), has(ModItems.COBBLESTONE_THIRTYNINE.get()))
                .save(pWriter, "decompressed_cobblestone_thirtynine");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THIRTYNINE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FORTY.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FORTY.get()), has(ModItems.COBBLESTONE_FORTY.get()))
                .save(pWriter, "decompressed_cobblestone_forty");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FORTY.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FORTYONE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FORTYONE.get()), has(ModItems.COBBLESTONE_FORTYONE.get()))
                .save(pWriter, "decompressed_cobblestone_fortyone");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FORTYONE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FORTYTWO.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FORTYTWO.get()), has(ModItems.COBBLESTONE_FORTYTWO.get()))
                .save(pWriter, "decompressed_cobblestone_fortytwo");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FORTYTWO.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FORTYTHREE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FORTYTHREE.get()), has(ModItems.COBBLESTONE_FORTYTHREE.get()))
                .save(pWriter, "decompressed_cobblestone_fortythree");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FORTYTHREE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FORTYFOUR.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FORTYFOUR.get()), has(ModItems.COBBLESTONE_FORTYFOUR.get()))
                .save(pWriter, "decompressed_cobblestone_fortyfour");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FORTYFOUR.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FORTYFIVE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FORTYFIVE.get()), has(ModItems.COBBLESTONE_FORTYFIVE.get()))
                .save(pWriter, "decompressed_cobblestone_fortyfive");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FORTYFIVE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FORTYSIX.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FORTYSIX.get()), has(ModItems.COBBLESTONE_FORTYSIX.get()))
                .save(pWriter, "decompressed_cobblestone_fortysix");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FORTYSIX.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FORTYSEVEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FORTYSEVEN.get()), has(ModItems.COBBLESTONE_FORTYSEVEN.get()))
                .save(pWriter, "decompressed_cobblestone_fortyseven");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FORTYSEVEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FORTYEIGHT.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FORTYEIGHT.get()), has(ModItems.COBBLESTONE_FORTYEIGHT.get()))
                .save(pWriter, "decompressed_cobblestone_fortyeight");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FORTYEIGHT.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FORTYNINE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FORTYNINE.get()), has(ModItems.COBBLESTONE_FORTYNINE.get()))
                .save(pWriter, "decompressed_cobblestone_fortynine");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FORTYNINE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FIFTY.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIFTY.get()), has(ModItems.COBBLESTONE_FIFTY.get()))
                .save(pWriter, "decompressed_cobblestone_fifty");
    }

}
