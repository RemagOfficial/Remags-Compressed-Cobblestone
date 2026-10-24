package com.remag.rcc.machine;

import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.energy.IBceStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The contract a {@link CompressionPortBlock} needs from whichever multiblock controller seals the centre of its
 * structure: forward an energy buffer (POWER ports), an item handler (ITEM_IN / ITEM_OUT), or a BCE store, per port
 * kind. Three machines answer through it - the 5x5x5 Compression Manipulator and the 3x3x3 Decompression Manipulator
 * (both full {@link IMultiblockController}s, which extends this) and the 3x3x4 tier-1 generator
 * ({@link GeneratorCoreBlockEntity}, which deliberately does <em>not</em> - it has no GUI and no compression
 * semantics, only ports). The controller itself is the only party that knows what each port kind means on its
 * machine: a fuel sink, a seed insert, or the one BCE-out drain.
 *
 * <p>Resolution is positional: a shell cell scans the {@link #MAX_REGION_RADIUS} cube around itself for any block
 * entity implementing this interface. Ports cache the found host's position exactly like they cached a controller
 * before; the GUI-facing {@link IMultiblockController#findController} stays separate so a menu is never opened on
 * the generator - the {@link CompressionInterfaceBlock} does notice a generator host, but answers a click on it
 * with a chat status readout instead of a screen.
 */
public interface IMultiblockPortHost {
    /**
     * The largest structure radius any controller can have (the Manipulator's {@code 5x5x5} half-width; the 3x3x3
     * and 3x3x4 towers sit entirely inside it). A shell cell scans this cube to find its host.
     */
    int MAX_REGION_RADIUS = 2;

    /** The FE buffer automation feeds through a POWER port, or {@code null} if the machine takes no power. */
    @Nullable
    IEnergyStorage getEnergy();

    /**
     * The item handler a port of {@code type} should expose, or {@code null} when this machine carries no items on
     * that kind (the POWER port on any machine; <em>every</em> port on a generator, which only moves FE in and BCE
     * out). The controller decides the semantics: the compressor's ITEM_IN is a fuel sink and its ITEM_OUT the
     * finished block, while the decompressor's ITEM_IN takes a seed to break down and its ITEM_OUT yields the
     * downgraded block.
     */
    @Nullable
    IItemHandler portItemHandler(PortType type);

    /**
     * The BCE store a port of {@code type} should expose, or {@code null} when that port kind carries no BCE. The
     * compressor offers a sink-only view of its fuel pool on ITEM_IN (a pipe burns BCE as fuel); the decompressor
     * offers its released-mass buffer on the item ports (two-way); the generator offers its minted pool on ITEM_OUT
     * only, as a one-way source - its designated single drain.
     */
    @Nullable
    IBceStorage portBceHandler(PortType type);

    /**
     * Locate the host whose structure contains the shell cell at {@code from}, scanning the {@link #MAX_REGION_RADIUS}
     * cube around it for any of the three registered core blocks. Overlapping multiblocks are not a supported
     * build, so first-match is sufficient; the block check up front keeps the wide scan cheap.
     */
    @Nullable
    static IMultiblockPortHost findPortHost(Level level, BlockPos from) {
        int r = MAX_REGION_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = from.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if ((s.is(ModBlocks.CORE.get()) || s.is(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get())
                            || s.is(ModBlocks.GENERATOR_CORE.get()))
                            && level.getBlockEntity(p) instanceof IMultiblockPortHost host) {
                        return host;
                    }
                }
            }
        }
        return null;
    }

    /** Convenience for port entities that need the host's position (cache invalidation, messages). */
    static @Nullable BlockPos hostPos(@Nullable IMultiblockPortHost host) {
        return host instanceof BlockEntity be ? be.getBlockPos() : null;
    }

    /**
     * Tell every cached capability answer inside the box this host could serve that it is no longer trustworthy.
     *
     * <p>A port answers a neighbour's query by resolving its host <em>at that instant</em>, and NeoForge caches the
     * reply per position ({@link net.neoforged.neoforge.capabilities.BlockCapabilityCache}) until something calls
     * {@link Level#invalidateCapabilities(BlockPos)}. Building a tower in the natural order - frame, ports, then the
     * cables and pipes, with the core placed <em>last</em> - therefore poisons the port: the cable cached
     * {@code null} while no core existed yet, and nothing ever asks again, because placing a core one or two blocks
     * away is not a block update <em>at the port's own position</em>. The machine then looks entirely dead (power
     * never arrives, the pool is never drained) until the chunk reloads and the caches are rebuilt - which reads to
     * the player as "it only started working after I relogged".
     *
     * <p>Each core calls this from {@link BlockEntity#onLoad()}, which fires on placement and on chunk load alike:
     * the first covers the last-block build order, the second a tower whose core chunk loads after the chunk holding
     * its cables. A port's own position needs no call when the port is placed or its type is re-cycled, since that is
     * a state change <em>at</em> the position and the level invalidates it by itself.
     */
    static void invalidatePortCaps(Level level, BlockPos corePos) {
        int r = MAX_REGION_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = corePos.offset(dx, dy, dz);
                    level.invalidateCapabilities(p);
                    // A pipe run keeps its own cached endpoint list, so tell any conduit touching a shell port to
                    // look again - otherwise it waits out its rescan TTL before the freshly-live store appears.
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
