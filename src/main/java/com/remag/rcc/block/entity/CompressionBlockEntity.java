package com.remag.rcc.block.entity;

import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.math.BigInteger;

/**
 * Stores the compression level of a single placed {@code compressed_cobblestone} block. One instance
 * replaces what used to be thousands of distinct blocks: the level is plain data, not a registry
 * entry.
 *
 * <p>The level is persisted to disk and, because {@link BlockEntity#getUpdateTag} serialises
 * {@link #saveAdditional}, automatically reaches clients so the block can be tinted per level. It is
 * also exposed as an implicit data component through {@link #collectImplicitComponents}, which the
 * {@code copy_components} loot function reads via {@link BlockEntity#collectComponents()} so a broken
 * block drops the item at the exact tier it was.
 */
public class CompressionBlockEntity extends BlockEntity {
    private static final String TAG_LEVEL = "level";

    private BigInteger compressionLevel = BigInteger.ONE;

    public CompressionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSION.get(), pos, state);
    }

    public BigInteger getCompressionLevel() {
        return compressionLevel;
    }

    public void setLevel(BigInteger value) {
        if (!this.compressionLevel.equals(value)) {
            this.compressionLevel = value;
            setChanged();
            // Re-send the update tag to clients and request a re-render so the tint follows the level.
            if (this.level != null && !this.level.isClientSide()) {
                BlockState state = getBlockState();
                this.level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }
    }

    /**
     * Called by {@link BlockEntity#collectComponents()} when the game builds the component map to
     * copy onto this block's drop. Adding the level here is what makes a broken block carry its tier.
     */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        builder.set(ModDataComponents.COMPRESSION_LEVEL.get(), this.compressionLevel);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(TAG_LEVEL, this.compressionLevel.toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.compressionLevel = tag.contains(TAG_LEVEL)
                ? new BigInteger(tag.getString(TAG_LEVEL))
                : BigInteger.ONE;
    }
}
