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

public class BceStorageBlockEntity extends BlockEntity {
    private static final String TAG_POOL = "bcePool";

    private double pool = Double.NEGATIVE_INFINITY;

    private double capacityLog() {
        return getBlockState().getBlock() instanceof BceStorageBlock tank
                ? tank.getCapacityLog()
                : CompressionConfig.BCE_STORAGE_CAPACITY_LOG;
    }

    private boolean infiniteSource() {
        return getBlockState().getBlock() instanceof BceStorageBlock tank && tank.isInfiniteSource();
    }

    private final IBceStorage view = new IBceStorage() {
        @Override
        public double getBceStoredLog() {
            return infiniteSource() ? BceStorageBlockEntity.this.capacityLog() : BceStorageBlockEntity.this.pool;
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
                return Double.NEGATIVE_INFINITY;
            }
            double accepted = Math.min(amountLog, free);
            if (!simulate) {
                double merged = CompressionConfig.log10add(BceStorageBlockEntity.this.pool, accepted);
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
            if (infiniteSource()) {
                return amountLog;
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
            return !infiniteSource();
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

    public Component contentsReport() {
        return Component.translatable("chat.rcc.bce_storage",
                DisplayNames.bceMagnitude(this.pool),
                DisplayNames.bceMagnitude(capacityLog()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble(TAG_POOL, this.pool);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.pool = tag.contains(TAG_POOL) ? tag.getDouble(TAG_POOL) : Double.NEGATIVE_INFINITY;
    }
}
