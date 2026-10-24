package com.remag.rcc.machine;

import com.remag.rcc.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The full controller contract of the two <em>compression</em> multiblocks - the 5x5x5 Compression Manipulator
 * ({@link CompressionCoreBlockEntity}) and the 3x3x3 Decompression Manipulator ({@link DecompressorCoreBlockEntity}).
 * It unifies the two halves automation and the GUI need: the port-facing capability forwarding lives in
 * {@link IMultiblockPortHost} (which this extends, so a port can also resolve a plain generator core without ever
 * seeing this type), while the machine state GUI already consumes comes from {@link ICompressionMachine} and the
 * {@link MenuProvider} the interface panel opens. Both controllers are block entities, so callers cast the resolved
 * instance to {@link BlockEntity} to read its position.
 */
public interface IMultiblockController extends ICompressionMachine, MenuProvider, IMultiblockPortHost {
    /**
     * Locate the compression controller whose structure contains the shell cell at {@code from}, scanning the
     * {@link IMultiblockPortHost#MAX_REGION_RADIUS} cube around it for either registered compression core block.
     * Used by the {@link CompressionInterfaceBlock} so a GUI can only ever resolve a machine that actually has one
     * - a {@link GeneratorCoreBlockEntity} answers ports, never menus. Two overlapping multiblocks are not a
     * supported build, so first-match is sufficient.
     */
    @Nullable
    static IMultiblockController findController(Level level, BlockPos from) {
        int r = IMultiblockPortHost.MAX_REGION_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = from.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if ((s.is(ModBlocks.CORE.get()) || s.is(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get()))
                            && level.getBlockEntity(p) instanceof IMultiblockController controller) {
                        return controller;
                    }
                }
            }
        }
        return null;
    }
}
