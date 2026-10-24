package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.IBceStorage;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A standalone BCE tank: the simplest possible {@link IBceStorage} - one log10 pool, one capacity, no slots, no GUI,
 * no ticking of its own. It exists so a pipe network has somewhere to put BCE before the multiblock store is built,
 * and so a decompressor's output survives the machine being broken. The capacity is <em>not</em> hardcoded here: it
 * comes from the {@link BceStorageBlock} the entity sits in, so the tier-1 tank
 * ({@link CompressionConfig#BCE_STORAGE_CAPACITY_LOG}, one tier-1000 block's yield) and the tier-2 tank
 * ({@link CompressionConfig#BCE_STORAGE_2_CAPACITY_LOG}, one Manipulator-band block's yield) share this one class and
 * this one block-entity type.
 *
 * <p>Unlike the decompressor's <em>producer</em> view (which refuses a write outright if it would overflow), this
 * view accepts <em>partially</em>: {@code receiveBce} returns the fraction of the request that actually fits, so a
 * bus can fill the tank right up to its ceiling instead of bouncing every oversized slice. The pipes' concentration
 * floor is what keeps small writes from vanishing; see {@link com.remag.rcc.energy.BceNetwork}.
 */
public class BceStorageBlockEntity extends BlockEntity {
    private static final String TAG_POOL = "bcePool";

    /** Accumulated BCE as a log10 mass; {@link Double#NEGATIVE_INFINITY} means empty. */
    private double pool = Double.NEGATIVE_INFINITY;

    /** This tank's ceiling: whichever {@link BceStorageBlock} variant placed this entity. */
    private double capacityLog() {
        return getBlockState().getBlock() instanceof BceStorageBlock tank
                ? tank.getCapacityLog()
                : CompressionConfig.BCE_STORAGE_CAPACITY_LOG; // defensive: only reachable pre-placement
    }

    /** The sided view handed to the capability registration; every face behaves identically. */
    private final IBceStorage view = new IBceStorage() {
        @Override
        public double getBceStoredLog() {
            return BceStorageBlockEntity.this.pool;
        }

        @Override
        public double getMaxBceLog() {
            return BceStorageBlockEntity.this.capacityLog();
        }

        @Override
        public double receiveBce(double amountLog, boolean simulate) {
            if (!canReceive() || amountLog == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            double cap = BceStorageBlockEntity.this.capacityLog();
            double free = CompressionConfig.log10subtract(cap, BceStorageBlockEntity.this.pool);
            if (free == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY; // full
            }
            double accepted = Math.min(amountLog, free); // partial fills are the point of this view
            if (!simulate) {
                double merged = CompressionConfig.log10add(BceStorageBlockEntity.this.pool, accepted);
                // Rounding can leave a hair over the ceiling; clamp so `free` returns NEGATIVE_INFINITY next time.
                BceStorageBlockEntity.this.pool = Math.min(merged, cap);
                setChanged();
            }
            return accepted;
        }

        @Override
        public double extractBce(double amountLog, boolean simulate) {
            if (!canExtract()) {
                return Double.NEGATIVE_INFINITY;
            }
            double poolNow = BceStorageBlockEntity.this.pool;
            if (poolNow == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            double take = Math.min(amountLog, poolNow);
            if (!simulate) {
                BceStorageBlockEntity.this.pool = CompressionConfig.log10subtract(poolNow, take);
                setChanged();
            }
            return take;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        @Override
        public boolean canExtract() {
            return true;
        }
    };

    public BceStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BCE_STORAGE.get(), pos, state);
    }

    public IBceStorage getBceStorage(@Nullable Direction side) {
        return this.view;
    }

    public double getStoredLog() {
        return this.pool;
    }

    /** The sneak-right-click readout: {@code BCE storage: 10^954 / 10^954} (against <em>this</em> tank's ceiling). */
    public Component contentsReport() {
        return Component.translatable("chat.rcc.bce_storage",
                DisplayNames.bceMagnitude(this.pool),
                DisplayNames.bceMagnitude(capacityLog()));
    }

    // --- Persistence -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble(TAG_POOL, this.pool);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // An absent key must stay empty rather than become log10(0) = -Infinity via a default double read.
        this.pool = tag.contains(TAG_POOL) ? tag.getDouble(TAG_POOL) : Double.NEGATIVE_INFINITY;
    }
}
