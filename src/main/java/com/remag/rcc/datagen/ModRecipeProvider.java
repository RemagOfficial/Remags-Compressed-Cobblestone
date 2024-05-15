package com.remag.rcc.datagen;

import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

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

        Item[] items = {
                ModItems.COBBLESTONE_ONE.get(), ModItems.COBBLESTONE_TWO.get(), ModItems.COBBLESTONE_THREE.get(), ModItems.COBBLESTONE_FOUR.get(), ModItems.COBBLESTONE_FIVE.get(),
                ModItems.COBBLESTONE_SIX.get(), ModItems.COBBLESTONE_SEVEN.get(), ModItems.COBBLESTONE_EIGHT.get(), ModItems.COBBLESTONE_NINE.get(), ModItems.COBBLESTONE_TEN.get(),
                ModItems.COBBLESTONE_ELEVEN.get(), ModItems.COBBLESTONE_TWELVE.get(), ModItems.COBBLESTONE_THIRTEEN.get(), ModItems.COBBLESTONE_FOURTEEN.get(), ModItems.COBBLESTONE_FIFTEEN.get(),
                ModItems.COBBLESTONE_SIXTEEN.get(), ModItems.COBBLESTONE_SEVENTEEN.get(), ModItems.COBBLESTONE_EIGHTEEN.get(), ModItems.COBBLESTONE_NINETEEN.get(), ModItems.COBBLESTONE_TWENTY.get(),
                ModItems.COBBLESTONE_TWENTYONE.get(), ModItems.COBBLESTONE_TWENTYTWO.get(), ModItems.COBBLESTONE_TWENTYTHREE.get(), ModItems.COBBLESTONE_TWENTYFOUR.get(), ModItems.COBBLESTONE_TWENTYFIVE.get(),
                ModItems.COBBLESTONE_TWENTYSIX.get(), ModItems.COBBLESTONE_TWENTYSEVEN.get(), ModItems.COBBLESTONE_TWENTYEIGHT.get(), ModItems.COBBLESTONE_TWENTYNINE.get(), ModItems.COBBLESTONE_THIRTY.get(),
                ModItems.COBBLESTONE_THIRTYONE.get(), ModItems.COBBLESTONE_THIRTYTWO.get(), ModItems.COBBLESTONE_THIRTYTHREE.get(), ModItems.COBBLESTONE_THIRTYFOUR.get(), ModItems.COBBLESTONE_THIRTYFIVE.get(),
                ModItems.COBBLESTONE_THIRTYSIX.get(), ModItems.COBBLESTONE_THIRTYSEVEN.get(), ModItems.COBBLESTONE_THIRTYEIGHT.get(), ModItems.COBBLESTONE_THIRTYNINE.get(), ModItems.COBBLESTONE_FORTY.get(),
                ModItems.COBBLESTONE_FORTYONE.get(), ModItems.COBBLESTONE_FORTYTWO.get(), ModItems.COBBLESTONE_FORTYTHREE.get(), ModItems.COBBLESTONE_FORTYFOUR.get(), ModItems.COBBLESTONE_FORTYFIVE.get(),
                ModItems.COBBLESTONE_FORTYSIX.get(), ModItems.COBBLESTONE_FORTYSEVEN.get(), ModItems.COBBLESTONE_FORTYEIGHT.get(), ModItems.COBBLESTONE_FORTYNINE.get(), ModItems.COBBLESTONE_FIFTY.get(),
                ModItems.COBBLESTONE_FIFTYONE.get(), ModItems.COBBLESTONE_FIFTYTWO.get(), ModItems.COBBLESTONE_FIFTYTHREE.get(), ModItems.COBBLESTONE_FIFTYFOUR.get(), ModItems.COBBLESTONE_FIFTYFIVE.get(),
                ModItems.COBBLESTONE_FIFTYSIX.get(), ModItems.COBBLESTONE_FIFTYSEVEN.get(), ModItems.COBBLESTONE_FIFTYEIGHT.get(), ModItems.COBBLESTONE_FIFTYNINE.get(), ModItems.COBBLESTONE_SIXTY.get(),
                ModItems.COBBLESTONE_SIXTYONE.get(), ModItems.COBBLESTONE_SIXTYTWO.get(), ModItems.COBBLESTONE_SIXTYTHREE.get(), ModItems.COBBLESTONE_SIXTYFOUR.get(), ModItems.COBBLESTONE_SIXTYFIVE.get(),
                ModItems.COBBLESTONE_SIXTYSIX.get(), ModItems.COBBLESTONE_SIXTYSEVEN.get(), ModItems.COBBLESTONE_SIXTYEIGHT.get(), ModItems.COBBLESTONE_SIXTYNINE.get(), ModItems.COBBLESTONE_SEVENTY.get(),
                ModItems.COBBLESTONE_SEVENTYONE.get(), ModItems.COBBLESTONE_SEVENTYTWO.get(), ModItems.COBBLESTONE_SEVENTYTHREE.get(), ModItems.COBBLESTONE_SEVENTYFOUR.get(), ModItems.COBBLESTONE_SEVENTYFIVE.get(),
                ModItems.COBBLESTONE_SEVENTYSIX.get(), ModItems.COBBLESTONE_SEVENTYSEVEN.get(), ModItems.COBBLESTONE_SEVENTYEIGHT.get(), ModItems.COBBLESTONE_SEVENTYNINE.get(), ModItems.COBBLESTONE_EIGHTY.get(),
                ModItems.COBBLESTONE_EIGHTYONE.get(), ModItems.COBBLESTONE_EIGHTYTWO.get(), ModItems.COBBLESTONE_EIGHTYTHREE.get(), ModItems.COBBLESTONE_EIGHTYFOUR.get(), ModItems.COBBLESTONE_EIGHTYFIVE.get(),
                ModItems.COBBLESTONE_EIGHTYSIX.get(), ModItems.COBBLESTONE_EIGHTYSEVEN.get(), ModItems.COBBLESTONE_EIGHTYEIGHT.get(), ModItems.COBBLESTONE_EIGHTYNINE.get(), ModItems.COBBLESTONE_NINETY.get(),
                ModItems.COBBLESTONE_NINETYONE.get(), ModItems.COBBLESTONE_NINETYTWO.get(), ModItems.COBBLESTONE_NINETYTHREE.get(), ModItems.COBBLESTONE_NINETYFOUR.get(), ModItems.COBBLESTONE_NINETYFIVE.get(),
                ModItems.COBBLESTONE_NINETYSIX.get(), ModItems.COBBLESTONE_NINETYSEVEN.get(), ModItems.COBBLESTONE_NINETYEIGHT.get(), ModItems.COBBLESTONE_NINETYNINE.get(), ModItems.COBBLESTONE_HUNDRED.get(),
                ModItems.COBBLESTONE_HUNDREDONE.get(), ModItems.COBBLESTONE_HUNDREDTWO.get(), ModItems.COBBLESTONE_HUNDREDTHREE.get(), ModItems.COBBLESTONE_HUNDREDFOUR.get(), ModItems.COBBLESTONE_HUNDREDFIVE.get()
        };
        Block[] blocks = {
                ModBlocks.COBBLESTONE_ONE.get(), ModBlocks.COBBLESTONE_TWO.get(), ModBlocks.COBBLESTONE_THREE.get(), ModBlocks.COBBLESTONE_FOUR.get(), ModBlocks.COBBLESTONE_FIVE.get(),
                ModBlocks.COBBLESTONE_SIX.get(), ModBlocks.COBBLESTONE_SEVEN.get(), ModBlocks.COBBLESTONE_EIGHT.get(), ModBlocks.COBBLESTONE_NINE.get(), ModBlocks.COBBLESTONE_TEN.get(),
                ModBlocks.COBBLESTONE_ELEVEN.get(), ModBlocks.COBBLESTONE_TWELVE.get(), ModBlocks.COBBLESTONE_THIRTEEN.get(), ModBlocks.COBBLESTONE_FOURTEEN.get(), ModBlocks.COBBLESTONE_FIFTEEN.get(),
                ModBlocks.COBBLESTONE_SIXTEEN.get(), ModBlocks.COBBLESTONE_SEVENTEEN.get(), ModBlocks.COBBLESTONE_EIGHTEEN.get(), ModBlocks.COBBLESTONE_NINETEEN.get(), ModBlocks.COBBLESTONE_TWENTY.get(),
                ModBlocks.COBBLESTONE_TWENTYONE.get(), ModBlocks.COBBLESTONE_TWENTYTWO.get(), ModBlocks.COBBLESTONE_TWENTYTHREE.get(), ModBlocks.COBBLESTONE_TWENTYFOUR.get(), ModBlocks.COBBLESTONE_TWENTYFIVE.get(),
                ModBlocks.COBBLESTONE_TWENTYSIX.get(), ModBlocks.COBBLESTONE_TWENTYSEVEN.get(), ModBlocks.COBBLESTONE_TWENTYEIGHT.get(), ModBlocks.COBBLESTONE_TWENTYNINE.get(), ModBlocks.COBBLESTONE_THIRTY.get(),
                ModBlocks.COBBLESTONE_THIRTYONE.get(), ModBlocks.COBBLESTONE_THIRTYTWO.get(), ModBlocks.COBBLESTONE_THIRTYTHREE.get(), ModBlocks.COBBLESTONE_THIRTYFOUR.get(), ModBlocks.COBBLESTONE_THIRTYFIVE.get(),
                ModBlocks.COBBLESTONE_THIRTYSIX.get(), ModBlocks.COBBLESTONE_THIRTYSEVEN.get(), ModBlocks.COBBLESTONE_THIRTYEIGHT.get(), ModBlocks.COBBLESTONE_THIRTYNINE.get(), ModBlocks.COBBLESTONE_FORTY.get(),
                ModBlocks.COBBLESTONE_FORTYONE.get(), ModBlocks.COBBLESTONE_FORTYTWO.get(), ModBlocks.COBBLESTONE_FORTYTHREE.get(), ModBlocks.COBBLESTONE_FORTYFOUR.get(), ModBlocks.COBBLESTONE_FORTYFIVE.get(),
                ModBlocks.COBBLESTONE_FORTYSIX.get(), ModBlocks.COBBLESTONE_FORTYSEVEN.get(), ModBlocks.COBBLESTONE_FORTYEIGHT.get(), ModBlocks.COBBLESTONE_FORTYNINE.get(), ModBlocks.COBBLESTONE_FIFTY.get(),
                ModBlocks.COBBLESTONE_FIFTYONE.get(), ModBlocks.COBBLESTONE_FIFTYTWO.get(), ModBlocks.COBBLESTONE_FIFTYTHREE.get(), ModBlocks.COBBLESTONE_FIFTYFOUR.get(), ModBlocks.COBBLESTONE_FIFTYFIVE.get(),
                ModBlocks.COBBLESTONE_FIFTYSIX.get(), ModBlocks.COBBLESTONE_FIFTYSEVEN.get(), ModBlocks.COBBLESTONE_FIFTYEIGHT.get(), ModBlocks.COBBLESTONE_FIFTYNINE.get(), ModBlocks.COBBLESTONE_SIXTY.get(),
                ModBlocks.COBBLESTONE_SIXTYONE.get(), ModBlocks.COBBLESTONE_SIXTYTWO.get(), ModBlocks.COBBLESTONE_SIXTYTHREE.get(), ModBlocks.COBBLESTONE_SIXTYFOUR.get(), ModBlocks.COBBLESTONE_SIXTYFIVE.get(),
                ModBlocks.COBBLESTONE_SIXTYSIX.get(), ModBlocks.COBBLESTONE_SIXTYSEVEN.get(), ModBlocks.COBBLESTONE_SIXTYEIGHT.get(), ModBlocks.COBBLESTONE_SIXTYNINE.get(), ModBlocks.COBBLESTONE_SEVENTY.get(),
                ModBlocks.COBBLESTONE_SEVENTYONE.get(), ModBlocks.COBBLESTONE_SEVENTYTWO.get(), ModBlocks.COBBLESTONE_SEVENTYTHREE.get(), ModBlocks.COBBLESTONE_SEVENTYFOUR.get(), ModBlocks.COBBLESTONE_SEVENTYFIVE.get(),
                ModBlocks.COBBLESTONE_SEVENTYSIX.get(), ModBlocks.COBBLESTONE_SEVENTYSEVEN.get(), ModBlocks.COBBLESTONE_SEVENTYEIGHT.get(), ModBlocks.COBBLESTONE_SEVENTYNINE.get(), ModBlocks.COBBLESTONE_EIGHTY.get(),
                ModBlocks.COBBLESTONE_EIGHTYONE.get(), ModBlocks.COBBLESTONE_EIGHTYTWO.get(), ModBlocks.COBBLESTONE_EIGHTYTHREE.get(), ModBlocks.COBBLESTONE_EIGHTYFOUR.get(), ModBlocks.COBBLESTONE_EIGHTYFIVE.get(),
                ModBlocks.COBBLESTONE_EIGHTYSIX.get(), ModBlocks.COBBLESTONE_EIGHTYSEVEN.get(), ModBlocks.COBBLESTONE_EIGHTYEIGHT.get(), ModBlocks.COBBLESTONE_EIGHTYNINE.get(), ModBlocks.COBBLESTONE_NINETY.get(),
                ModBlocks.COBBLESTONE_NINETYONE.get(), ModBlocks.COBBLESTONE_NINETYTWO.get(), ModBlocks.COBBLESTONE_NINETYTHREE.get(), ModBlocks.COBBLESTONE_NINETYFOUR.get(), ModBlocks.COBBLESTONE_NINETYFIVE.get(),
                ModBlocks.COBBLESTONE_NINETYSIX.get(), ModBlocks.COBBLESTONE_NINETYSEVEN.get(), ModBlocks.COBBLESTONE_NINETYEIGHT.get(), ModBlocks.COBBLESTONE_NINETYNINE.get(), ModBlocks.COBBLESTONE_HUNDRED.get(),
                ModBlocks.COBBLESTONE_HUNDREDONE.get(), ModBlocks.COBBLESTONE_HUNDREDTWO.get(), ModBlocks.COBBLESTONE_HUNDREDTHREE.get(), ModBlocks.COBBLESTONE_HUNDREDFOUR.get(), ModBlocks.COBBLESTONE_HUNDREDFIVE.get()
        };
        // an array of spelt numbers from 1 to 105
        String[] speltNumbers = {
                "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
                "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
                "twenty", "twentyone", "twentytwo", "twentythree", "twentyfour", "twentyfive", "twentysix", "twentyseven", "twentyeight", "twentynine",
                "thirty", "thirtyone", "thirtytwo", "thirtythree", "thirtyfour", "thirtyfive", "thirtysix", "thirtyseven", "thirtyeight", "thirtynine",
                "forty", "fortyone", "fortytwo", "fortythree", "fortyfour", "fortyfive", "fortysix", "fortyseven", "fortyeight", "fortynine",
                "fifty", "fiftyone", "fiftytwo", "fiftythree", "fiftyfour", "fiftyfive", "fiftysix", "fiftyseven", "fiftyeight", "fiftynine",
                "sixty", "sixtyone", "sixtytwo", "sixtythree", "sixtyfour", "sixtyfive", "sixtysix", "sixtyseven", "sixtyeight", "sixtynine",
                "seventy", "seventyone", "seventytwo", "seventythree", "seventyfour", "seventyfive", "seventysix", "seventyseven", "seventyeight", "seventynine",
                "eighty", "eightyone", "eightytwo", "eightythree", "eightyfour", "eightyfive", "eightysix", "eightyseven", "eightyeight", "eightynine",
                "ninety", "ninetyone", "ninetytwo", "ninetythree", "ninetyfour", "ninetyfive", "ninetysix", "ninetyseven", "ninetyeight", "ninetynine",
                "hundred", "hundredone", "hundredtwo", "hundredthree", "hundredfour", "hundredfive"
        };
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

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIFTYONE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FIFTYTWO.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIFTYTWO.get()), has(ModItems.COBBLESTONE_FIFTYTWO.get()))
                .save(pWriter, "decompressed_cobblestone_fiftyone");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIFTYTWO.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FIFTYTHREE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIFTYTHREE.get()), has(ModItems.COBBLESTONE_FIFTYTHREE.get()))
                .save(pWriter, "decompressed_cobblestone_fiftytwo");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIFTYTHREE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FIFTYFOUR.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIFTYFOUR.get()), has(ModItems.COBBLESTONE_FIFTYFOUR.get()))
                .save(pWriter, "decompressed_cobblestone_fiftythree");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIFTYFOUR.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FIFTYFIVE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIFTYFIVE.get()), has(ModItems.COBBLESTONE_FIFTYFIVE.get()))
                .save(pWriter, "decompressed_cobblestone_fiftyfour");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIFTYFIVE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FIFTYSIX.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIFTYSIX.get()), has(ModItems.COBBLESTONE_FIFTYSIX.get()))
                .save(pWriter, "decompressed_cobblestone_fiftyfive");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIFTYSIX.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FIFTYSEVEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIFTYSEVEN.get()), has(ModItems.COBBLESTONE_FIFTYSEVEN.get()))
                .save(pWriter, "decompressed_cobblestone_fiftysix");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIFTYSEVEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FIFTYEIGHT.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIFTYEIGHT.get()), has(ModItems.COBBLESTONE_FIFTYEIGHT.get()))
                .save(pWriter, "decompressed_cobblestone_fiftyseven");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIFTYEIGHT.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_FIFTYNINE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIFTYNINE.get()), has(ModItems.COBBLESTONE_FIFTYNINE.get()))
                .save(pWriter, "decompressed_cobblestone_fiftyeight");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIFTYNINE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SIXTY.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIXTY.get()), has(ModItems.COBBLESTONE_SIXTY.get()))
                .save(pWriter, "decompressed_cobblestone_fifty-nine");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIXTY.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SIXTYONE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIXTYONE.get()), has(ModItems.COBBLESTONE_SIXTYONE.get()))
                .save(pWriter, "decompressed_cobblestone_sixty");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIXTYONE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SIXTYTWO.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIXTYTWO.get()), has(ModItems.COBBLESTONE_SIXTYTWO.get()))
                .save(pWriter, "decompressed_cobblestone_sixtyone");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIXTYTWO.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SIXTYTHREE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIXTYTHREE.get()), has(ModItems.COBBLESTONE_SIXTYTHREE.get()))
                .save(pWriter, "decompressed_cobblestone_sixtytwo");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIXTYTHREE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SIXTYFOUR.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIXTYFOUR.get()), has(ModItems.COBBLESTONE_SIXTYFOUR.get()))
                .save(pWriter, "decompressed_cobblestone_sixtythree");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIXTYFOUR.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SIXTYFIVE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIXTYFIVE.get()), has(ModItems.COBBLESTONE_SIXTYFIVE.get()))
                .save(pWriter, "decompressed_cobblestone_sixtyfour");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIXTYFIVE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SIXTYSIX.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIXTYSIX.get()), has(ModItems.COBBLESTONE_SIXTYSIX.get()))
                .save(pWriter, "decompressed_cobblestone_sixtyfive");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIXTYSIX.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SIXTYSEVEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIXTYSEVEN.get()), has(ModItems.COBBLESTONE_SIXTYSEVEN.get()))
                .save(pWriter, "decompressed_cobblestone_sixtysix");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIXTYSEVEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SIXTYEIGHT.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIXTYEIGHT.get()), has(ModItems.COBBLESTONE_SIXTYEIGHT.get()))
                .save(pWriter, "decompressed_cobblestone_sixtyseven");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIXTYEIGHT.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SIXTYNINE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIXTYNINE.get()), has(ModItems.COBBLESTONE_SIXTYNINE.get()))
                .save(pWriter, "decompressed_cobblestone_sixtyeight");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIXTYNINE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SEVENTY.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVENTY.get()), has(ModItems.COBBLESTONE_SEVENTY.get()))
                .save(pWriter, "decompressed_cobblestone_sixtynine");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVENTY.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SEVENTYONE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVENTYONE.get()), has(ModItems.COBBLESTONE_SEVENTYONE.get()))
                .save(pWriter, "decompressed_cobblestone_seventy");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVENTYONE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SEVENTYTWO.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVENTYTWO.get()), has(ModItems.COBBLESTONE_SEVENTYTWO.get()))
                .save(pWriter, "decompressed_cobblestone_seventyone");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVENTYTWO.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SEVENTYTHREE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVENTYTHREE.get()), has(ModItems.COBBLESTONE_SEVENTYTHREE.get()))
                .save(pWriter, "decompressed_cobblestone_seventytwo");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVENTYTHREE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SEVENTYFOUR.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVENTYFOUR.get()), has(ModItems.COBBLESTONE_SEVENTYFOUR.get()))
                .save(pWriter, "decompressed_cobblestone_seventythree");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVENTYFOUR.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SEVENTYFIVE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVENTYFIVE.get()), has(ModItems.COBBLESTONE_SEVENTYFIVE.get()))
                .save(pWriter, "decompressed_cobblestone_seventyfour");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVENTYFIVE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SEVENTYSIX.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVENTYSIX.get()), has(ModItems.COBBLESTONE_SEVENTYSIX.get()))
                .save(pWriter, "decompressed_cobblestone_seventyfive");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVENTYSIX.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SEVENTYSEVEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVENTYSEVEN.get()), has(ModItems.COBBLESTONE_SEVENTYSEVEN.get()))
                .save(pWriter, "decompressed_cobblestone_seventysix");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVENTYSEVEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SEVENTYEIGHT.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVENTYEIGHT.get()), has(ModItems.COBBLESTONE_SEVENTYEIGHT.get()))
                .save(pWriter, "decompressed_cobblestone_seventyseven");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVENTYEIGHT.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_SEVENTYNINE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVENTYNINE.get()), has(ModItems.COBBLESTONE_SEVENTYNINE.get()))
                .save(pWriter, "decompressed_cobblestone_seventyeight");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVENTYNINE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_EIGHTY.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHTY.get()), has(ModItems.COBBLESTONE_EIGHTY.get()))
                .save(pWriter, "decompressed_cobblestone_seventynine");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHTY.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_EIGHTYONE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHTYONE.get()), has(ModItems.COBBLESTONE_EIGHTYONE.get()))
                .save(pWriter, "decompressed_cobblestone_eighty");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHTYONE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_EIGHTYTWO.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHTYTWO.get()), has(ModItems.COBBLESTONE_EIGHTYTWO.get()))
                .save(pWriter, "decompressed_cobblestone_eightyone");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHTYTWO.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_EIGHTYTHREE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHTYTHREE.get()), has(ModItems.COBBLESTONE_EIGHTYTHREE.get()))
                .save(pWriter, "decompressed_cobblestone_eightytwo");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHTYTHREE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_EIGHTYFOUR.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHTYFOUR.get()), has(ModItems.COBBLESTONE_EIGHTYFOUR.get()))
                .save(pWriter, "decompressed_cobblestone_eightythree");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHTYFOUR.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_EIGHTYFIVE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHTYFIVE.get()), has(ModItems.COBBLESTONE_EIGHTYFIVE.get()))
                .save(pWriter, "decompressed_cobblestone_eightyfour");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHTYFIVE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_EIGHTYSIX.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHTYSIX.get()), has(ModItems.COBBLESTONE_EIGHTYSIX.get()))
                .save(pWriter, "decompressed_cobblestone_eightyfive");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHTYSIX.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_EIGHTYSEVEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHTYSEVEN.get()), has(ModItems.COBBLESTONE_EIGHTYSEVEN.get()))
                .save(pWriter, "decompressed_cobblestone_eightysix");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHTYSEVEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_EIGHTYEIGHT.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHTYEIGHT.get()), has(ModItems.COBBLESTONE_EIGHTYEIGHT.get()))
                .save(pWriter, "decompressed_cobblestone_eightyseven");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHTYEIGHT.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_EIGHTYNINE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHTYNINE.get()), has(ModItems.COBBLESTONE_EIGHTYNINE.get()))
                .save(pWriter, "decompressed_cobblestone_eightyeight");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHTYNINE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_NINETY.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINETY.get()), has(ModItems.COBBLESTONE_NINETY.get()))
                .save(pWriter, "decompressed_cobblestone_eightynine");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINETY.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_NINETYONE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINETYONE.get()), has(ModItems.COBBLESTONE_NINETYONE.get()))
                .save(pWriter, "decompressed_cobblestone_ninety");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINETYONE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_NINETYTWO.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINETYTWO.get()), has(ModItems.COBBLESTONE_NINETYTWO.get()))
                .save(pWriter, "decompressed_cobblestone_ninetyone");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINETYTWO.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_NINETYTHREE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINETYTHREE.get()), has(ModItems.COBBLESTONE_NINETYTHREE.get()))
                .save(pWriter, "decompressed_cobblestone_ninetytwo");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINETYTHREE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_NINETYFOUR.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINETYFOUR.get()), has(ModItems.COBBLESTONE_NINETYFOUR.get()))
                .save(pWriter, "decompressed_cobblestone_ninetythree");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINETYFOUR.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_NINETYFIVE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINETYFIVE.get()), has(ModItems.COBBLESTONE_NINETYFIVE.get()))
                .save(pWriter, "decompressed_cobblestone_ninetyfour");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINETYFIVE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_NINETYSIX.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINETYSIX.get()), has(ModItems.COBBLESTONE_NINETYSIX.get()))
                .save(pWriter, "decompressed_cobblestone_ninetyfive");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINETYSIX.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_NINETYSEVEN.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINETYSEVEN.get()), has(ModItems.COBBLESTONE_NINETYSEVEN.get()))
                .save(pWriter, "decompressed_cobblestone_ninetysix");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINETYSEVEN.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_NINETYEIGHT.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINETYEIGHT.get()), has(ModItems.COBBLESTONE_NINETYEIGHT.get()))
                .save(pWriter, "decompressed_cobblestone_ninetyseven");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINETYEIGHT.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_NINETYNINE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINETYNINE.get()), has(ModItems.COBBLESTONE_NINETYNINE.get()))
                .save(pWriter, "decompressed_cobblestone_ninetyeight");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINETYNINE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_HUNDRED.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_HUNDRED.get()), has(ModItems.COBBLESTONE_HUNDRED.get()))
                .save(pWriter, "decompressed_cobblestone_ninetynine");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_HUNDRED.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_HUNDREDONE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_HUNDREDONE.get()), has(ModItems.COBBLESTONE_HUNDREDONE.get()))
                .save(pWriter, "decompressed_cobblestone_hundred");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_HUNDREDONE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_HUNDREDTWO.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_HUNDREDTWO.get()), has(ModItems.COBBLESTONE_HUNDREDTWO.get()))
                .save(pWriter, "decompressed_cobblestone_hundredone");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_HUNDREDTWO.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_HUNDREDTHREE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_HUNDREDTHREE.get()), has(ModItems.COBBLESTONE_HUNDREDTHREE.get()))
                .save(pWriter, "decompressed_cobblestone_hundredtwo");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_HUNDREDTHREE.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_HUNDREDFOUR.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_HUNDREDFOUR.get()), has(ModItems.COBBLESTONE_HUNDREDFOUR.get()))
                .save(pWriter, "decompressed_cobblestone_hundredthree");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_HUNDREDFOUR.get(), 9)
                .group("decompress")
                .requires(ModItems.COBBLESTONE_HUNDREDFIVE.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_HUNDREDFIVE.get()), has(ModItems.COBBLESTONE_HUNDREDFIVE.get()))
                .save(pWriter, "decompressed_cobblestone_hundredfour");
    }

}
