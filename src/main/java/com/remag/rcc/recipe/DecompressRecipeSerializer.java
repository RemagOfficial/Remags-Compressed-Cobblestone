package com.remag.rcc.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class DecompressRecipeSerializer implements RecipeSerializer<DecompressRecipe> {
    private static final DecompressRecipe PROTOTYPE = new DecompressRecipe();

    @Override
    public MapCodec<DecompressRecipe> codec() {
        return MapCodec.unit(PROTOTYPE);
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, DecompressRecipe> streamCodec() {
        return StreamCodec.unit(PROTOTYPE);
    }
}
