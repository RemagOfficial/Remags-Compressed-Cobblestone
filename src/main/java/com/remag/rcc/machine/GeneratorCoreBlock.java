package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.registry.Banded;
import com.remag.rcc.registry.CompressionBand;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.BlockItemStateProperties;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class GeneratorCoreBlock extends Block implements EntityBlock, Banded {
    public static final IntegerProperty RUNG =
            IntegerProperty.create("rung", 0, CompressionBand.maxRungCount() - 1);

    private final CompressionBand band;

    public GeneratorCoreBlock(BlockBehaviour.Properties properties) {
        this(properties, CompressionBand.BAND_1);
    }

    public GeneratorCoreBlock(BlockBehaviour.Properties properties, CompressionBand band) {
        super(properties);
        this.band = band;
        this.registerDefaultState(this.stateDefinition.any().setValue(RUNG, 0));
    }

    @Override
    public CompressionBand band() {
        return this.band;
    }

    public GeneratorCoreTier rungFor(BlockState state) {
        return new GeneratorCoreTier(this.band, state.getValue(RUNG));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorCoreBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RUNG);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int rung = 0;
        BlockItemStateProperties stateProps = context.getItemInHand().get(DataComponents.BLOCK_STATE);
        if (stateProps != null) {
            Integer fromItem = stateProps.get(RUNG);
            if (fromItem != null) {
                rung = fromItem;
            }
        }
        int lastRung = Math.max(0, this.band.rungExponents().size() - 1);
        return this.defaultBlockState().setValue(RUNG, Math.min(Math.max(rung, 0), lastRung));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.GENERATOR_CORE.get(),
                GeneratorCoreBlockEntity::serverTick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> shouldBeType, BlockEntityType<E> isType, BlockEntityTicker<? super E> ticker) {
        return isType == shouldBeType ? (BlockEntityTicker<A>) ticker : null;
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
        if (level.getBlockEntity(pos) instanceof GeneratorCoreBlockEntity core) {
            player.displayClientMessage(Component.translatable("chat.rcc.generator_core.status",
                    Component.literal(rungFor(state).displaySuffix()).withStyle(ChatFormatting.GOLD),
                    Component.literal(core.contentsSummary()).withStyle(ChatFormatting.GRAY)), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
