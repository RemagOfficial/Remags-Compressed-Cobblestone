package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.BceNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BcePipeBlockEntity extends BlockEntity {
    @Nullable
    private BceNetwork network;

    public BcePipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BCE_PIPE.get(), pos, state);
    }

    @Nullable
    public BceNetwork getNetwork() {
        return this.network;
    }

    public void setNetwork(@Nullable BceNetwork network) {
        this.network = network;
    }

    public void markNetworkStale() {
        if (this.network != null) {
            this.network.markStale();
        }
        this.network = null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BcePipeBlockEntity pipe) {
        BceNetwork network = BceNetwork.networkFor(pipe);
        if (network != null) {
            network.tick();
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        builder.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY
                .with(BcePipeBlock.TIER, getBlockState().getValue(BcePipeBlock.TIER)));
    }
}
