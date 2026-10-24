package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Auto-Compressor: a single-block machine that automates the manual tier ladder up to
 * {@link com.remag.rcc.registry.CompressionConfig#AUTO_TARGET_LEVEL}. Feed it FE, any-tier cobblestone (fuel) and a
 * seed, and it repeatedly upgrades the seed one tier at a time until it reaches the target, then ejects the result.
 *
 * <p>Unlike the sealed multiblock core this block is directly interactive: right-clicking it opens the shared
 * {@link com.remag.rcc.menu.CompressionCoreMenu}. It carries a {@link BlockStateProperties#HORIZONTAL_FACING}
 * state so the interface {@code front} panel turns to face the player on placement. All logic is
 * server-authoritative; the client only mirrors synced state through that menu's {@code ContainerData}.
 */
public class CompressionAutoBlock extends Block implements EntityBlock {

    public CompressionAutoBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    /** Point the interface front at the player: the block faces back along their line of sight. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompressionAutoBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // All machine logic is server-authoritative; the client needs no ticker.
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.COMPRESSION_AUTO.get(), CompressionAutoBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS; // the server opens the menu
        }
        if (level.getBlockEntity(pos) instanceof CompressionAutoBlockEntity machine) {
            // Write the position AND the current goal tier into the open buffer so the client screen can prefill the
            // goal-tier text field with the server-authoritative value (mirrors the Auto Decompressor).
            player.openMenu(machine, buf -> {
                buf.writeBlockPos(pos);
                buf.writeUtf(machine.goalTierString());
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> shouldBeType, BlockEntityType<E> isType, BlockEntityTicker<? super E> ticker) {
        return isType == shouldBeType ? (BlockEntityTicker<A>) ticker : null;
    }
}
