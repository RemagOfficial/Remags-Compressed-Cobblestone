package com.remag.rcc.machine;

import com.remag.rcc.registry.CompressionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A standalone BCE tank block. Passive by design: it never ticks, all flow is driven by the
 * {@link com.remag.rcc.energy.BceNetwork} that touches it, exactly like the multiblock's
 * {@link CompressionPortBlock} defers to the core.
 *
 * <p>One class covers the whole tank ladder: the capacity (log10 BCE) is a constructor parameter, so
 * {@code BCE Storage} (one tier-1000 block, {@link CompressionConfig#BCE_STORAGE_CAPACITY_LOG}) and
 * {@code BCE Storage II} (one tier-{@link CompressionConfig#MANIPULATOR_MAX_OUT} block,
 * {@link CompressionConfig#BCE_STORAGE_2_CAPACITY_LOG}) are two registrations of this block sharing one
 * {@link BceStorageBlockEntity} type - the entity simply asks its block how big its pool is.
 *
 * <p>Interaction is deliberately minimal (no GUI, no new menu): sneak-right-click prints the stored amount in chat,
 * anything else returns {@link InteractionResult#PASS} so the block never gets in the way of placing pipes against it
 * or of other mods' automation reaching its {@link com.remag.rcc.energy.IBceStorage} capability.
 */
public class BceStorageBlock extends Block implements EntityBlock {

    /** Ceiling of this tank's pool, in log10 BCE. */
    private final double capacityLog;

    public BceStorageBlock(BlockBehaviour.Properties properties) {
        this(properties, CompressionConfig.BCE_STORAGE_CAPACITY_LOG);
    }

    public BceStorageBlock(BlockBehaviour.Properties properties, double capacityLog) {
        super(properties);
        this.capacityLog = capacityLog;
    }

    public double getCapacityLog() {
        return this.capacityLog;
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
            return InteractionResult.PASS; // only the sneak gesture reports; otherwise leave the block alone
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
