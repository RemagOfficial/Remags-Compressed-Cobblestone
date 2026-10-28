package com.remag.rcc.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ModItemTags {
    public static final TagKey<Item> WRENCH =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    public static final TagKey<Item> WRENCH_LEGACY =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "wrenches"));

    public static boolean isWrench(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(WRENCH) || stack.is(WRENCH_LEGACY));
    }

    private ModItemTags() {
    }
}
