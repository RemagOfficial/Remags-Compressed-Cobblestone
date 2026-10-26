package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
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

/**
 * The tier-1 Generation Manipulator's centre block: the sealed heart of the 3x3x4 generator tower. One block covers
 * the whole ladder through a {@link GeneratorCoreTier} {@link #RUNG} block-state property (the same single-block
 * multi-variant shape as {@link CobblestoneGeneratorBlock}'s tier), and the rung rides in and out of placement on
 * the stack's {@code block_state} component.
 *
 * <p>Like the other multiblock cores it is <em>not</em> a machine you interact with to build - the structure is
 * assembled block by block in the world and validated by {@link GeneratorCoreBlockEntity#scanRegion()}. It has no
 * GUI: the tower's required {@link CompressionInterfaceBlock} panel is the normal way to read its status, since the
 * core ends up fully sealed inside the shell (where a sneak right-click still works if you expose it, but is not
 * the intended gesture). Whether a valid tower surrounds the core is block-entity state only, never a blockstate,
 * so the block keeps a single model.
 */
public class GeneratorCoreBlock extends Block implements EntityBlock {
    /**
     * The rung index within this block's band ladder, a bounded {@code int} rather than a fixed enum so a new band's
     * differently-sized ladder needs no new property constants (see {@link GeneratorCoreTier} for how an index and a
     * band resolve to a rung's rate, share and FE bill). The bound is the widest ladder in the
     * {@link CompressionBand} table; a narrower band simply never places the high indices.
     */
    public static final IntegerProperty RUNG =
            IntegerProperty.create("rung", 0, CompressionBand.maxRungCount() - 1);

    /** The progression band whose rung ladder this core block runs; one class serves every band. */
    private final CompressionBand band;

    public GeneratorCoreBlock(BlockBehaviour.Properties properties) {
        this(properties, CompressionBand.BAND_1);
    }

    public GeneratorCoreBlock(BlockBehaviour.Properties properties, CompressionBand band) {
        super(properties);
        this.band = band;
        this.registerDefaultState(this.stateDefinition.any().setValue(RUNG, 0));
    }

    /** The band whose rung ladder and array divisor this core's rungs are drawn from. */
    public CompressionBand band() {
        return this.band;
    }

    /** Resolve the rung this state runs on: this block's band and the {@link #RUNG} index it carries. */
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

    /**
     * Place on the rung index carried on the held stack's {@code block_state} component (the creative-tab per-rung
     * entries and any broken-and-replaced core), defaulting to the band's base rung (index 0) when the stack names
     * none - the {@link CobblestoneGeneratorBlock#getStateForPlacement} route. The index is clamped to this band's
     * own ladder length so a foreign high index can never aim a narrower band's core off the end of its rungs.
     */
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
        // Minting and the structure rescan are server-authoritative; the client needs no ticker.
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
        // Only the sneak gesture does anything; otherwise leave the block for placing against.
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
