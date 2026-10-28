package com.remag.rcc.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class AutoItemHandler implements IItemHandler {
    private final CompressionAutoBlockEntity machine;

    public AutoItemHandler(CompressionAutoBlockEntity machine) {
        this.machine = machine;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return machine.getOutput().getStackInSlot(0);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!ICompressionMachine.isFuel(stack)) {
            return stack;
        }
        if (!simulate) {
            machine.acceptFuel(stack);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return machine.getOutput().extractItem(0, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return ICompressionMachine.isFuel(stack);
    }
}
