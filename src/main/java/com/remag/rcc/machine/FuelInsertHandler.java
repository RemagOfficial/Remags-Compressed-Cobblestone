package com.remag.rcc.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A single-slot, insert-only view of a {@link CompressionCoreBlockEntity} presented by an ITEM_IN port. Feeding
 * any fuel stack adds its base-cobblestone-equivalent mass to the core's log10 pool and consumes the whole
 * stack; nothing can be extracted back out (mass, once compressed into the pool, is spent).
 */
public class FuelInsertHandler implements IItemHandler {
    private final CompressionCoreBlockEntity core;

    public FuelInsertHandler(CompressionCoreBlockEntity core) {
        this.core = core;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!CompressionCoreBlockEntity.isFuel(stack)) {
            return stack; // not fuel: reject the whole stack as leftover
        }
        if (!simulate) {
            core.acceptFuel(stack);
        }
        return ItemStack.EMPTY; // accepted entirely (a fuel sink has no visible contents)
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return CompressionCoreBlockEntity.isFuel(stack);
    }
}
