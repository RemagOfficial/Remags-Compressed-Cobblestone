package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity behind a {@link CompressionPortBlock}. It stores nothing itself: it locates the multiblock
 * {@link CompressionCoreBlockEntity} (the core is always within the structure radius of any shell cell) and
 * exposes, per {@link PortType}, either the core's energy buffer (POWER), a fuel sink that feeds the core's
 * mass pool (ITEM_IN), or the core's output handler (ITEM_OUT).
 */
public class CompressionPortBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos cachedCorePos = null;

    public CompressionPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSION_PORT.get(), pos, state);
    }

    private PortType portType() {
        return getBlockState().getValue(CompressionPortBlock.PORT_TYPE);
    }

    /** Find (and cache) the core this port belongs to, re-scanning if the cached one is gone. */
    @Nullable
    public CompressionCoreBlockEntity resolveCore() {
        Level level = this.level;
        if (level == null) {
            return null;
        }
        if (cachedCorePos != null && level.getBlockEntity(cachedCorePos) instanceof CompressionCoreBlockEntity cached) {
            return cached;
        }
        cachedCorePos = null;
        int r = com.remag.rcc.registry.CompressionConfig.REGION_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    if (level.getBlockState(p).is(com.remag.rcc.block.ModBlocks.CORE.get())
                            && level.getBlockEntity(p) instanceof CompressionCoreBlockEntity core) {
                        cachedCorePos = p;
                        return core;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        if (portType() != PortType.POWER) {
            return null;
        }
        CompressionCoreBlockEntity core = resolveCore();
        return core == null ? null : core.getEnergy();
    }

    @Nullable
    public IItemHandler getItemCapability(@Nullable Direction side) {
        CompressionCoreBlockEntity core = resolveCore();
        if (core == null) {
            return null;
        }
        return switch (portType()) {
            case ITEM_OUT -> core.getOutput();
            case ITEM_IN -> new FuelInsertHandler(core);
            case POWER -> null;
        };
    }
}
