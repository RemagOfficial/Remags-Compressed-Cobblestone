package com.remag.rcc.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A single-slot, insert-only seed feeder presented by a decompressor multiblock's ITEM_IN port. The mirror of
 * {@link FuelInsertHandler} for the reverse machine: rather than burning a fed stack into a pool, it deposits a valid
 * compressed-cobblestone <em>seed</em> into the controller's seed slot to be stepped down. Only a tiered block is
 * accepted (plain cobblestone has nothing to decompress), and nothing can be extracted back out here - pulling the
 * finished downgraded block is the ITEM_OUT port's job.
 */
public class SeedInsertHandler implements IItemHandler {
    private final DecompressorCoreBlockEntity machine;

    public SeedInsertHandler(DecompressorCoreBlockEntity machine) {
        this.machine = machine;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        // Input-only view: the seed being broken down is not surfaced as extractable content here.
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!ICompressionMachine.isSeed(stack)) {
            return stack; // only a compressed block can be a seed; reject the whole stack as leftover
        }
        return machine.getSeed().insertItem(0, stack, simulate);
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
        return ICompressionMachine.isSeed(stack);
    }
}
