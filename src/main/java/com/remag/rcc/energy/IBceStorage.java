package com.remag.rcc.energy;

/**
 * A transferable store of Base Cobblestone Equivalent (BCE) mass. Unlike items or FE energy, BCE amounts can be
 * astronomically large (a tier-1000 block is worth {@code 9^1000} vanilla cobblestone), so every magnitude here is
 * carried as a base-10 logarithm (a {@code double}) - the same representation the machines use for their internal
 * fuel pools. Amounts are log10 masses; {@link Double#NEGATIVE_INFINITY} means empty.
 *
 * <p>Exposed as a NeoForge block capability (see {@link com.remag.rcc.registry.ModCapabilities#BCE}), which is what
 * {@link BceNetwork} queries to find the stores at its ends. Implemented by the two stores in the mod: the Auto
 * Decompressor's buffer (a producer view, refusing a write that would overflow) and the standalone
 * {@link com.remag.rcc.machine.BceStorageBlockEntity} tank (a view that accepts as much as fits).
 */
public interface IBceStorage {
    /** Current stored BCE mass as a log10 value; {@link Double#NEGATIVE_INFINITY} when empty. */
    double getBceStoredLog();

    /** Capacity - the largest log10 BCE mass this store can hold. */
    double getMaxBceLog();

    /**
     * Try to add {@code amountLog} (a log10 mass) of BCE. Returns the log10 amount actually accepted, or
     * {@link Double#NEGATIVE_INFINITY} if none fit (the store would overflow).
     */
    double receiveBce(double amountLog, boolean simulate);

    /**
     * Try to remove up to {@code amountLog} (a log10 mass) of BCE. Returns the log10 amount actually extracted, or
     * {@link Double#NEGATIVE_INFINITY} if the store is empty.
     */
    double extractBce(double amountLog, boolean simulate);

    boolean canReceive();

    boolean canExtract();
}
