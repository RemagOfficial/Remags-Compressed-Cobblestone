package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CobblestoneGeneratorBlock extends Block implements EntityBlock {
    public static final EnumProperty<GeneratorTier> TIER = EnumProperty.create("tier", GeneratorTier.class);

    public CobblestoneGeneratorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TIER, GeneratorTier.BASE));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CobblestoneGeneratorBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        GeneratorTier tier = GeneratorTier.BASE;
        BlockItemStateProperties stateProps = context.getItemInHand().get(DataComponents.BLOCK_STATE);
        if (stateProps != null) {
            GeneratorTier fromItem = stateProps.get(TIER);
            if (fromItem != null) {
                tier = fromItem;
            }
        }
        return this.defaultBlockState().setValue(TIER, tier);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.COBBLESTONE_GENERATOR.get(),
                CobblestoneGeneratorBlockEntity::serverTick);
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
        if (level.getBlockEntity(pos) instanceof CobblestoneGeneratorBlockEntity gen) {
            GeneratorTier next = cycle(state.getValue(TIER));
            level.setBlock(pos, state.setValue(TIER, next), Block.UPDATE_ALL);
            gen.onTierChanged(level.getBlockState(pos));
            player.displayClientMessage(Component.translatable("chat.rcc.generator.cycled",
                    Component.literal(next.getSerializedName()).withStyle(ChatFormatting.GOLD),
                    Component.literal(gen.contentsSummary()).withStyle(ChatFormatting.GRAY)), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static GeneratorTier cycle(GeneratorTier current) {
        GeneratorTier[] all = GeneratorTier.values();
        return all[(current.ordinal() + 1) % all.length];
    }
}
