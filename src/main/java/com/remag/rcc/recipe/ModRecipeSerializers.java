package com.remag.rcc.recipe;

import com.remag.rcc.RCC;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the two dynamic recipe serializers. Both recipes report {@link net.minecraft.world.item.crafting.RecipeType#CRAFTING}
 * so they live in the crafting-table recipe map and need no custom {@code RecipeType} of their own.
 */
public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, RCC.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, CompressRecipeSerializer> COMPRESS =
            SERIALIZERS.register("compress", CompressRecipeSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, DecompressRecipeSerializer> DECOMPRESS =
            SERIALIZERS.register("decompress", DecompressRecipeSerializer::new);

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
