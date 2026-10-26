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
 * The tier-1 Decompression Manipulator's centre block and its 3x3x3 multiblock controller. Like the Compression
 * Manipulator's {@link CompressionCoreBlock} it owns the machine state (seed slot, BCE buffer, energy, goal tier,
 * descent cycle) and seals it in the structure's core: it is <em>not</em> directly interactive - the player reaches
 * its GUI through a {@link CompressionInterfaceBlock} and the outside world connects through {@link CompressionPortBlock}s.
 * Whether a valid 3x3x3 structure surrounds it is tracked purely as block-entity state
 * ({@link DecompressorCoreBlockEntity#isFormed()}), never a blockstate, so the block keeps a single model.
 */
public class DecompressorCoreBlock extends Block implements EntityBlock {

    /**
     * The progression band this decompressor core belongs to; its row's {@link CompressionBand#decompressorRadius()}
     * drives the structure scan and the {@link CompressionBand#sinkCeilingLog()} sizes the released-mass buffer. One
     * class serves every band ({@link BceStorageBlock}'s idiom); the single-argument constructor keeps the shipped
     * band-1 registration reading as before.
     */
    private final CompressionBand band;

    public DecompressorCoreBlock(BlockBehaviour.Properties properties) {
        this(properties, CompressionBand.BAND_1);
    }

    public DecompressorCoreBlock(BlockBehaviour.Properties properties, CompressionBand band) {
        super(properties);
        this.band = band;
    }

    /** The band whose table row drives this core's structure validation. */
    public CompressionBand band() {
        return this.band;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DecompressorCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // All machine logic (including the structure rescan) is server-authoritative; the client renders synced state.
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.COMPRESSION_DECOMPRESSOR_CORE.get(),
                        DecompressorCoreBlockEntity::serverTick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> shouldBeType, BlockEntityType<E> isType, BlockEntityTicker<? super E> ticker) {
        return isType == shouldBeType ? (BlockEntityTicker<A>) ticker : null;
    }
}
