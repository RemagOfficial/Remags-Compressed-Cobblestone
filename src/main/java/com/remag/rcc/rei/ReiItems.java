package com.remag.rcc.rei;

import com.remag.rcc.item.ModItems;
import com.remag.rcc.registry.ModDataComponents;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

final class ReiItems {

    private ReiItems() {
    }

    static ItemStack levelStack(BigInteger tier) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), tier);
        return stack;
    }

    static EntryStack<ItemStack> entry(ItemStack stack) {
        return EntryStack.of(VanillaEntryTypes.ITEM, stack);
    }

    static List<Item> allCompressionCores() {
        List<Item> cores = new ArrayList<>();
        cores.add(ModItems.COMPRESSION_CORE.get());
        cores.add(ModItems.COMPRESSION_CORE_2.get());
        for (var deferred : ModItems.COMPRESSION_CORES_BY_BAND.values()) {
            cores.add(deferred.get());
        }
        return cores;
    }

    static EntryIngredient ingredientOf(Item item) {
        return EntryIngredient.of(entry(new ItemStack(item)));
    }

    static EntryIngredient ingredientOfAll(List<Item> items) {
        EntryIngredient.Builder builder = EntryIngredient.builder();
        for (Item item : items) {
            builder.add(entry(new ItemStack(item)));
        }
        return builder.build();
    }
}
