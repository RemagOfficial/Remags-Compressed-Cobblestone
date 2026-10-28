package com.remag.rcc.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    public static final TagKey<Block> BCE_CONNECTABLE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("rcc", "bce_connectable"));

    private ModBlockTags() {
    }
}
