package com.remag.rcc.machine;

import com.remag.rcc.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The surface the shared {@link com.remag.rcc.menu.CompressionCoreMenu} and {@link
 * com.remag.rcc.client.CompressionCoreScreen} need from any compression machine, so the multiblock core and the
 * single-block Auto-Compressor can drive the same GUI (seed slot, output slot, and the fuel / progress / energy
 * gauges) without the menu knowing which machine it is bound to.
 *
 * <p>Everything here is the display/inventory contract only. Machine-specific behaviour (the core's
 * render/pulse bookkeeping, the Auto-Compressor's tier ladder) stays on the concrete block entities.
 */
public interface ICompressionMachine {
    /** The seed slot (the block being compressed / upgraded in place). */
    ItemStackHandler getSeed();

    /** The finished-output slot; may hold a stack of results. */
    ItemStackHandler getOutput();

    /** The machine's energy buffer, exposed to automation as a receive-only storage. */
    IEnergyStorage getEnergy();

    /** Current cycle/step progress, {@code 0} when idle, otherwise counting up to {@link #getProgressMax()}. */
    int getProgress();

    /** Progress value at which the current cycle/step completes (drives the progress gauge's full height). */
    int getProgressMax();

    /** Accumulated fuel mass in log10-base-cobblestone space; {@link Double#NEGATIVE_INFINITY} means empty. */
    double getFuelPool();

    /** Fuel requirement (log10 mass) for the currently loaded seed, or {@code 0} when there is no valid seed. */
    double getRequiredFuel();

    /** Whether the machine is ready to run (the multiblock's structure check; always {@code true} for the Auto-Compressor). */
    boolean isFormed();

    /** Add a fed stack's base-cobblestone-equivalent mass to the fuel pool. */
    void acceptFuel(ItemStack stack);

    /**
     * Whether {@code stack} may sit in this machine's seed slot. Defaults to any compressed-cobblestone tier
     * ({@link #isSeed}); the Auto-Compressor widens this to also accept plain vanilla cobblestone as the tier-1
     * starting seed, so the first compression no longer has to be done by hand.
     */
    default boolean acceptsSeed(ItemStack stack) {
        return isSeed(stack);
    }

    /** Only a compressed-cobblestone item may sit in a seed slot (vanilla cobblestone is fuel-only). */
    static boolean isSeed(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.COMPRESSED_COBBLESTONE.get());
    }

    /** A stack usable as fuel: any compressed-cobblestone tier, or plain vanilla cobblestone. */
    static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.COMPRESSED_COBBLESTONE.get()) || stack.is(Items.COBBLESTONE));
    }
}
