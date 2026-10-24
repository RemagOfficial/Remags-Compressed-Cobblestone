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
 * The Auto Decompressor: a single-block machine that reverses the Auto-Compressor. Feed it FE and a compressed
 * seed, pick a goal tier in its GUI, and it steps the seed down tier-by-tier, banking the released mass into a
 * {@link com.remag.rcc.energy.IBceStorage BCE} buffer and ejecting the downgraded block once it reaches the goal.
 *
 * <p>Like the compressor this block is directly interactive (it is its own GUI entry point) and carries a
 * {@link BlockStateProperties#HORIZONTAL_FACING} state so the interface {@code front} panel turns to face the player
 * on placement. It exposes energy / item / BCE capabilities on every face; all logic is server-authoritative and the
 * client mirrors synced state through the shared menu's {@code ContainerData}.
 */
public class CompressionAutoDecompressorBlock extends Block implements EntityBlock {

    public CompressionAutoDecompressorBlock(BlockBehaviour.Properties properties) {
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
        return new CompressionAutoDecompressorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // All machine logic is server-authoritative; the client needs no ticker.
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.COMPRESSION_AUTO_DECOMPRESSOR.get(),
                        CompressionAutoDecompressorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS; // the server opens the menu
        }
        if (level.getBlockEntity(pos) instanceof CompressionAutoDecompressorBlockEntity machine) {
            // Write the facing position AND the current goal tier into the open buffer so the client screen can
            // prefill the goal-tier text field with the server-authoritative value.
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
