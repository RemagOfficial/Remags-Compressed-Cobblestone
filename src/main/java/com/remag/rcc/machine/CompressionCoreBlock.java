package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.registry.CompressionBand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Compression Manipulator's centre block and the multiblock's controller. It owns the seed slot,
 * the fuel-mass pool, the energy buffer, the output slot and the craft cycle. Whether a valid 5x5x5
 * structure surrounds it is tracked purely as block-entity state ({@link CompressionCoreBlockEntity#isFormed()}),
 * never as a blockstate property, so the block keeps a single model in every case.
 */
public class CompressionCoreBlock extends Block implements EntityBlock {

    /**
     * The progression band this core's structure is built to. One class serves every band, so the geometry radius,
     * port floor and tier ceilings the block entity validates against are read from this row rather than from the
     * band-1 aliases in {@link com.remag.rcc.registry.CompressionConfig} - the same carries-a-value idiom as
     * {@link BceStorageBlock}'s pool size. The single-argument constructor keeps the shipped band-1 registration
     * (and every existing reference) reading exactly as before.
     */
    private final CompressionBand band;

    public CompressionCoreBlock(BlockBehaviour.Properties properties) {
        this(properties, CompressionBand.BAND_1);
    }

    public CompressionCoreBlock(BlockBehaviour.Properties properties, CompressionBand band) {
        super(properties);
        this.band = band;
    }

    /** The band whose table row drives this core's structure validation and ceilings. */
    public CompressionBand band() {
        return this.band;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompressionCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // All machine logic is server-authoritative; the client only renders synced state.
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.COMPRESSION_CORE.get(), CompressionCoreBlockEntity::serverTick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> shouldBeType, BlockEntityType<E> isType, BlockEntityTicker<? super E> ticker) {
        return isType == shouldBeType ? (BlockEntityTicker<A>) ticker : null;
    }
}
