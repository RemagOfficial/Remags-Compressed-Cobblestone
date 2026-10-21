package com.remag.rcc.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Serializer for {@link CompressRecipe}. The recipe carries no per-tier data (the level is derived from
 * the grid at craft time), so the codecs are stateless unit codecs and a single shared instance is reused.
 * Its JSON is simply {@code {"type": "rcc:compress"}}.
 */
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
