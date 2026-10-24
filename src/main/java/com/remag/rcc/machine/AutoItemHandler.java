package com.remag.rcc.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The single item capability exposed on every face of the {@link CompressionAutoBlockEntity}. It merges the two
 * automation jobs a machine needs into one handler so no dedicated port blocks are required:
 *
 * <ul>
 *   <li>{@code insertItem} treats anything that {@link ICompressionMachine#isFuel is fuel} as a mass contribution
 *       to the machine's pool and consumes the whole stack (a fuel sink, mirroring the multiblock's ITEM_IN
 *       {@link FuelInsertHandler});</li>
 *   <li>{@code getStackInSlot}/{@code extractItem} read and pull the machine's finished {@code output} slot, so a
 *       hopper or pipe on any side can eject the completed tier-1000 block.</li>
 * </ul>
 *
 * <p>Insert and extract therefore target different internal stores - this is the deliberate tradeoff of giving a
 * single-block machine all three inputs (fuel in / product out / energy in) on every face without separate ports.
 */
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
        // The visible/extractable content is the finished output; the fuel pool is not a stored item.
        return machine.getOutput().getStackInSlot(0);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!ICompressionMachine.isFuel(stack)) {
            return stack; // not fuel: reject the whole stack as leftover
        }
        if (!simulate) {
            machine.acceptFuel(stack);
        }
        return ItemStack.EMPTY; // accepted entirely (a fuel sink keeps no visible contents)
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
