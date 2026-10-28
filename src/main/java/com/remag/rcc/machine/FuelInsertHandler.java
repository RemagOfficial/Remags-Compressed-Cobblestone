package com.remag.rcc.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

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
            return stack;
        }
        if (!simulate) {
            core.acceptFuel(stack);
        }
        return ItemStack.EMPTY;
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
