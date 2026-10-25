package com.remag.rcc.machine;

import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The full controller contract of the two <em>compression</em> multiblocks - the 5x5x5 Compression Manipulator
 * ({@link CompressionCoreBlockEntity}) and the 3x3x3 Decompression Manipulator ({@link DecompressorCoreBlockEntity}).
 * It unifies the two halves automation and the GUI need: the port-facing capability forwarding lives in
 * {@link IMultiblockPortHost} (which this extends, so a port can also resolve a plain generator core without ever
 * seeing this type), while the machine state GUI already consumes comes from {@link ICompressionMachine} and the
 * {@link MenuProvider} the interface panel opens. Both controllers are block entities, so callers cast the resolved
 * instance to {@link BlockEntity} to read its position.
 *
 * <p>Implementing this type <em>is</em> the answer to "does clicking a panel here open a screen?":
 * {@link CompressionInterfaceBlock} resolves the nearest {@link IMultiblockPortHost} and dispatches on it, so there is
 * deliberately no controller-only lookup here. An {@code findController}-style probe used to be that lookup, and it
 * was a hazard rather than a helper - asked for a GUI-bearing core, it stepped straight over a nearer
 * {@link GeneratorCoreBlockEntity} (which is not a controller) and bound the panel to whatever compression machine sat
 * further away, up to the widest band's reach.
 */
public interface IMultiblockController extends ICompressionMachine, MenuProvider, IMultiblockPortHost {
}
