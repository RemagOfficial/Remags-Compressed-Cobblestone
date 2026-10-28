package com.remag.rcc.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CompressionInterfaceBlock extends Block {
    public CompressionInterfaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        IMultiblockPortHost host = IMultiblockPortHost.findPortHost(level, pos);
        if (host instanceof GeneratorCoreBlockEntity generator) {
            player.displayClientMessage(Component.translatable("chat.rcc.generator_core.status",
                    Component.literal(generator.rung().displaySuffix()).withStyle(ChatFormatting.GOLD),
                    Component.literal(generator.contentsSummary()).withStyle(ChatFormatting.GRAY)), false);
            return InteractionResult.SUCCESS;
        }
        if (!(host instanceof IMultiblockController controller)) {
            player.displayClientMessage(Component.translatable("container.rcc.compression_core.detached"), true);
            return InteractionResult.FAIL;
        }
        if (!controller.isFormed()) {
            player.displayClientMessage(Component.translatable("container.rcc.compression_core.unformed"), true);
            return InteractionResult.FAIL;
        }
        BlockPos corePos = ((BlockEntity) controller).getBlockPos();
        player.openMenu(controller, buf -> {
            buf.writeBlockPos(corePos);
            if (controller instanceof IGoalTierTarget target) {
                buf.writeUtf(target.goalTierString());
            }
        });
        return InteractionResult.SUCCESS;
    }
}
