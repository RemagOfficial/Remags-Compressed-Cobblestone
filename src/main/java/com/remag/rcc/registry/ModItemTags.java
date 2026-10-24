package com.remag.rcc.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The item tags RCC shares with other mods, and the one judgement they exist to make: "is this thing in the
 * player's hand a wrench?".
 *
 * <p>There is no wrench <em>interface</em> in NeoForge - the cross-mod contract is a convention tag. RCC therefore
 * consumes {@code c:tools/wrench} (so any wrench already in the pack configures our pipes) and publishes onto the
 * same tag in datagen (so our wrench configures <em>theirs</em>). Players with a modpack full of pipes do not have to
 * hunt for the one right tool: this is the same tag Mekanism's Configurator, Create's wrench and Supplementaries'
 * wrench all live behind.
 */
public final class ModItemTags {
    /** The NeoForge/Fabric convention tool tag every modern wrench is expected to carry. */
    public static final TagKey<Item> WRENCH =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    /**
     * The pre-convention Forge spelling, still emitted by mods that were ported rather than rewritten. Matched only
     * so an old wrench keeps working; nothing in 1.21.1 publishes it.
     */
    public static final TagKey<Item> WRENCH_LEGACY =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "wrenches"));

    /** True when the held stack is a wrench by convention - ours or any other mod's. */
    public static boolean isWrench(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(WRENCH) || stack.is(WRENCH_LEGACY));
    }

    private ModItemTags() {
    }
}
