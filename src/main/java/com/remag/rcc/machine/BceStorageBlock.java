package com.remag.rcc.machine;

import com.remag.rcc.registry.CompressionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class BceStorageBlock extends Block implements EntityBlock {

    private final double capacityLog;

    private final boolean infiniteSource;

    public BceStorageBlock(BlockBehaviour.Properties properties) {
        this(properties, CompressionConfig.BCE_STORAGE_CAPACITY_LOG);
    }

    public BceStorageBlock(BlockBehaviour.Properties properties, double capacityLog) {
        this(properties, capacityLog, false);
    }

    public BceStorageBlock(BlockBehaviour.Properties properties, double capacityLog, boolean infiniteSource) {
        super(properties);
        this.capacityLog = capacityLog;
        this.infiniteSource = infiniteSource;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    public double getCapacityLog() {
        return this.capacityLog;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    public boolean isInfiniteSource() {
        return this.infiniteSource;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BceStorageBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof BceStorageBlockEntity tank) {
            player.displayClientMessage(tank.contentsReport(), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
