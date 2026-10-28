package com.remag.rcc.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class CompressRecipeSerializer implements RecipeSerializer<CompressRecipe> {
    private static final CompressRecipe PROTOTYPE = new CompressRecipe();

    @Override
    public MapCodec<CompressRecipe> codec() {
        return MapCodec.unit(PROTOTYPE);
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CompressRecipe> streamCodec() {
        return StreamCodec.unit(PROTOTYPE);
    }
}
