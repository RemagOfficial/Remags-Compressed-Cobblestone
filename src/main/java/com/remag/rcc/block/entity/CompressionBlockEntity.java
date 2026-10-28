package com.remag.rcc.block.entity;

import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.math.BigInteger;

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
            if (this.level != null && !this.level.isClientSide()) {
                BlockState state = getBlockState();
                this.level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }
    }

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

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_LEVEL, this.compressionLevel.toString());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
