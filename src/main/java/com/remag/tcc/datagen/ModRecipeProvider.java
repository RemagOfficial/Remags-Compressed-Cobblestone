package com.remag.tcc.datagen;

import com.remag.tcc.block.ModBlocks;
import com.remag.tcc.item.ModItems;
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

        Item[] items = {ModItems.COBBLESTONE_ONE_ITEM.get(), ModItems.COBBLESTONE_TWO_ITEM.get(), ModItems.COBBLESTONE_THREE_ITEM.get(), ModItems.COBBLESTONE_FOUR_ITEM.get(), ModItems.COBBLESTONE_FIVE_ITEM.get(), ModItems.COBBLESTONE_SIX_ITEM.get(), ModItems.COBBLESTONE_SEVEN_ITEM.get(), ModItems.COBBLESTONE_EIGHT_ITEM.get(), ModItems.COBBLESTONE_NINE_ITEM.get(), ModItems.COBBLESTONE_TEN_ITEM.get(), ModItems.COBBLESTONE_ELEVEN_ITEM.get(), ModItems.COBBLESTONE_TWELVE_ITEM.get(), ModItems.COBBLESTONE_THIRTEEN_ITEM.get(), ModItems.COBBLESTONE_FOURTEEN_ITEM.get(), ModItems.COBBLESTONE_FIFTEEN_ITEM.get(), ModItems.COBBLESTONE_SIXTEEN_ITEM.get(), ModItems.COBBLESTONE_SEVENTEEN_ITEM.get(), ModItems.COBBLESTONE_EIGHTEEN_ITEM.get(), ModItems.COBBLESTONE_NINETEEN_ITEM.get(), ModItems.COBBLESTONE_TWENTY_ITEM.get(), ModItems.COBBLESTONE_TWENTYONE_ITEM.get(), ModItems.COBBLESTONE_TWENTYTWO_ITEM.get(), ModItems.COBBLESTONE_TWENTYTHREE_ITEM.get()};

        Block[] blocks = {ModBlocks.COBBLESTONE_ONE.get(), ModBlocks.COBBLESTONE_TWO.get(), ModBlocks.COBBLESTONE_THREE.get(), ModBlocks.COBBLESTONE_FOUR.get(), ModBlocks.COBBLESTONE_FIVE.get(), ModBlocks.COBBLESTONE_SIX.get(), ModBlocks.COBBLESTONE_SEVEN.get(), ModBlocks.COBBLESTONE_EIGHT.get(), ModBlocks.COBBLESTONE_NINE.get(), ModBlocks.COBBLESTONE_TEN.get(), ModBlocks.COBBLESTONE_ELEVEN.get(), ModBlocks.COBBLESTONE_TWELVE.get(), ModBlocks.COBBLESTONE_THIRTEEN.get(), ModBlocks.COBBLESTONE_FOURTEEN.get(), ModBlocks.COBBLESTONE_FIFTEEN.get(), ModBlocks.COBBLESTONE_SIXTEEN.get(), ModBlocks.COBBLESTONE_SEVENTEEN.get(), ModBlocks.COBBLESTONE_EIGHTEEN.get(), ModBlocks.COBBLESTONE_NINETEEN.get(), ModBlocks.COBBLESTONE_TWENTY.get(), ModBlocks.COBBLESTONE_TWENTYONE.get(), ModBlocks.COBBLESTONE_TWENTYTWO.get(), ModBlocks.COBBLESTONE_TWENTYTHREE.get()};

        for (int i = 0; i < items.length - 1; i++) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, blocks[i])
                    .define('#', items[i])
                    .pattern("###")
                    .pattern("###")
                    .pattern("###")
                    .unlockedBy(getHasName(items[i + 1]), has(items[i + 1]))
                    .save(pWriter);
        }

        // uncraft recipes for cobblestone

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Blocks.COBBLESTONE, 9)
                .requires(ModItems.COBBLESTONE_ONE_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_ONE_ITEM.get()), has(ModItems.COBBLESTONE_ONE_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_ONE.get(), 9)
                .requires(ModItems.COBBLESTONE_TWO_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWO_ITEM.get()), has(ModItems.COBBLESTONE_TWO_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWO.get(), 9)
                .requires(ModItems.COBBLESTONE_THREE_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THREE_ITEM.get()), has(ModItems.COBBLESTONE_THREE_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THREE.get(), 9)
                .requires(ModItems.COBBLESTONE_FOUR_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FOUR_ITEM.get()), has(ModItems.COBBLESTONE_FOUR_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FOUR.get(), 9)
                .requires(ModItems.COBBLESTONE_FIVE_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIVE_ITEM.get()), has(ModItems.COBBLESTONE_FIVE_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIVE.get(), 9)
                .requires(ModItems.COBBLESTONE_SIX_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIX_ITEM.get()), has(ModItems.COBBLESTONE_SIX_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIX.get(), 9)
                .requires(ModItems.COBBLESTONE_SEVEN_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVEN_ITEM.get()), has(ModItems.COBBLESTONE_SEVEN_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVEN.get(), 9)
                .requires(ModItems.COBBLESTONE_EIGHT_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHT_ITEM.get()), has(ModItems.COBBLESTONE_EIGHT_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHT.get(), 9)
                .requires(ModItems.COBBLESTONE_NINE_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINE_ITEM.get()), has(ModItems.COBBLESTONE_NINE_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINE.get(), 9)
                .requires(ModItems.COBBLESTONE_TEN_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TEN_ITEM.get()), has(ModItems.COBBLESTONE_TEN_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TEN.get(), 9)
                .requires(ModItems.COBBLESTONE_ELEVEN_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_ELEVEN_ITEM.get()), has(ModItems.COBBLESTONE_ELEVEN_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_ELEVEN.get(), 9)
                .requires(ModItems.COBBLESTONE_TWELVE_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWELVE_ITEM.get()), has(ModItems.COBBLESTONE_TWELVE_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWELVE.get(), 9)
                .requires(ModItems.COBBLESTONE_THIRTEEN_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_THIRTEEN_ITEM.get()), has(ModItems.COBBLESTONE_THIRTEEN_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_THIRTEEN.get(), 9)
                .requires(ModItems.COBBLESTONE_FOURTEEN_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FOURTEEN_ITEM.get()), has(ModItems.COBBLESTONE_FOURTEEN_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FOURTEEN.get(), 9)
                .requires(ModItems.COBBLESTONE_FIFTEEN_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_FIFTEEN_ITEM.get()), has(ModItems.COBBLESTONE_FIFTEEN_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_FIFTEEN.get(), 9)
                .requires(ModItems.COBBLESTONE_SIXTEEN_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SIXTEEN_ITEM.get()), has(ModItems.COBBLESTONE_SIXTEEN_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SIXTEEN.get(), 9)
                .requires(ModItems.COBBLESTONE_SEVENTEEN_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_SEVENTEEN_ITEM.get()), has(ModItems.COBBLESTONE_SEVENTEEN_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_SEVENTEEN.get(), 9)
                .requires(ModItems.COBBLESTONE_EIGHTEEN_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_EIGHTEEN_ITEM.get()), has(ModItems.COBBLESTONE_EIGHTEEN_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_EIGHTEEN.get(), 9)
                .requires(ModItems.COBBLESTONE_NINETEEN_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_NINETEEN_ITEM.get()), has(ModItems.COBBLESTONE_NINETEEN_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_NINETEEN.get(), 9)
                .requires(ModItems.COBBLESTONE_TWENTY_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTY_ITEM.get()), has(ModItems.COBBLESTONE_TWENTY_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTY.get(), 9)
                .requires(ModItems.COBBLESTONE_TWENTYONE_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTYONE_ITEM.get()), has(ModItems.COBBLESTONE_TWENTYONE_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTYONE.get(), 9)
                .requires(ModItems.COBBLESTONE_TWENTYTWO_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTYTWO_ITEM.get()), has(ModItems.COBBLESTONE_TWENTYTWO_ITEM.get()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.COBBLESTONE_TWENTYTWO.get(), 9)
                .requires(ModItems.COBBLESTONE_TWENTYTHREE_ITEM.get())
                .unlockedBy(getHasName(ModItems.COBBLESTONE_TWENTYTHREE_ITEM.get()), has(ModItems.COBBLESTONE_TWENTYTHREE_ITEM.get()))
                .save(pWriter);
    }

}
