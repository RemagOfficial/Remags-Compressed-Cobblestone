package com.remag.rcc.machine;

import com.remag.rcc.energy.IBceStorage;
import com.remag.rcc.registry.CompressionBand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface IMultiblockPortHost {
    int MAX_SHELL_EXTENT = CompressionBand.maxShellExtent();

    BlockPos[] CORE_SCAN_OFFSETS = buildCoreScanOffsets();

    private static BlockPos[] buildCoreScanOffsets() {
        int r = MAX_SHELL_EXTENT;
        int diameter = 2 * r + 1;
        List<BlockPos> offsets = new ArrayList<>(diameter * diameter * diameter);
        for (int ring = 0; ring <= r; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dy = -ring; dy <= ring; dy++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) == ring) {
                            offsets.add(new BlockPos(dx, dy, dz));
                        }
                    }
                }
            }
        }
        return offsets.toArray(new BlockPos[0]);
    }

    @Nullable
    IEnergyStorage getEnergy();

    @Nullable
    IItemHandler portItemHandler(PortType type);

    @Nullable
    IBceStorage portBceHandler(PortType type);

    default int portShellExtent() {
        return CompressionBand.BAND_1.shellExtent();
    }

    @Nullable
    static IMultiblockPortHost findPortHost(Level level, BlockPos from) {
        for (BlockPos off : CORE_SCAN_OFFSETS) {
            BlockPos p = off.offset(from);
            if (!level.isLoaded(p)) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            if ((s.getBlock() instanceof CompressionCoreBlock || s.getBlock() instanceof DecompressorCoreBlock
                    || s.getBlock() instanceof GeneratorCoreBlock)
                    && level.getBlockEntity(p) instanceof IMultiblockPortHost host) {
                return host;
            }
        }
        return null;
    }

    static @Nullable BlockPos hostPos(@Nullable IMultiblockPortHost host) {
        return host instanceof BlockEntity be ? be.getBlockPos() : null;
    }

    static List<IMultiblockPortHost> findGeneratorPowerHosts(Level level, BlockPos from) {
        List<IMultiblockPortHost> hosts = new ArrayList<>();
        for (BlockPos off : CORE_SCAN_OFFSETS) {
            BlockPos p = off.offset(from);
            if (!level.isLoaded(p)) {
                continue;
            }
            if (level.getBlockState(p).getBlock() instanceof GeneratorCoreBlock
                    && level.getBlockEntity(p) instanceof GeneratorCoreBlockEntity core
                    && core.claimsPowerPort(from)) {
                hosts.add(core);
            }
        }
        return hosts;
    }

    static void invalidatePortCaps(Level level, BlockPos corePos, int extent) {
        for (int dx = -extent; dx <= extent; dx++) {
            for (int dy = -extent; dy <= extent; dy++) {
                for (int dz = -extent; dz <= extent; dz++) {
                    BlockPos p = corePos.offset(dx, dy, dz);
                    if (!level.isLoaded(p)) {
                        continue;
                    }
                    level.invalidateCapabilities(p);
                    if (level.getBlockEntity(p) instanceof CompressionPortBlockEntity) {
                        for (Direction dir : Direction.values()) {
                            if (level.getBlockEntity(p.relative(dir)) instanceof BcePipeBlockEntity pipe) {
                                pipe.markNetworkStale();
                            }
                        }
                    }
                }
            }
        }
    }
}
