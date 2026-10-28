package com.remag.rcc.machine;

import com.remag.rcc.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public interface ICompressionMachine {
    ItemStackHandler getSeed();

    ItemStackHandler getOutput();

    IEnergyStorage getEnergy();

    int getProgress();

    int getProgressMax();

    double getFuelPool();

    double getRequiredFuel();

    boolean isFormed();

    default int guiShellRadius() {
        return 0;
    }

    void acceptFuel(ItemStack stack);

    default boolean acceptsSeed(ItemStack stack) {
        return isSeed(stack);
    }

    enum CraftStatus {
        READY,
        NOT_FORMED,
        NO_SEED,
        SEED_TOO_LOW,
        SEED_AT_CAP,
        OUTPUT_FULL,
        NEED_FUEL,
        NEED_ENERGY
    }

    default CraftStatus craftStatus() {
        return CraftStatus.READY;
    }

    static boolean isSeed(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.COMPRESSED_COBBLESTONE.get());
    }

    static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.COMPRESSED_COBBLESTONE.get()) || stack.is(Items.COBBLESTONE));
    }
}
