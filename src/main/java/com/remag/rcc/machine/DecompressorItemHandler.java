package com.remag.rcc.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The item capability exposed on every face of the {@link CompressionAutoDecompressorBlockEntity}. It mirrors the
 * Auto-Compressor's combined {@link AutoItemHandler}, inverted for the reverse machine:
 *
 * <ul>
 *   <li>{@code insertItem} pushes a compressed-cobblestone <em>seed</em> into the seed slot (only a tiered block has
 *       anything to decompress, so plain cobblestone is rejected);</li>
 *   <li>{@code getStackInSlot}/{@code extractItem} read and pull the finished downgraded block from the output slot.</li>
 * </ul>
 *
 * <p>The BCE product is deliberately not an item here - it leaves through the separate
 * {@link com.remag.rcc.energy.IBceStorage} capability, keeping "mass out" distinct from "block out".
 */
public class DecompressorItemHandler implements IItemHandler {
    private final CompressionAutoDecompressorBlockEntity machine;

    public DecompressorItemHandler(CompressionAutoDecompressorBlockEntity machine) {
        this.machine = machine;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        // The visible/extractable content is the finished downgraded output; the BCE buffer is not a stored item.
        return machine.getOutput().getStackInSlot(0);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!ICompressionMachine.isSeed(stack)) {
            return stack; // only a compressed block can be a seed (vanilla has nothing to decompress)
        }
        return machine.getSeed().insertItem(0, stack, simulate);
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
        return ICompressionMachine.isSeed(stack);
    }
}
