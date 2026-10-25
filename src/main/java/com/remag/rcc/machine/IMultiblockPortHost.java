package com.remag.rcc.machine;

import com.remag.rcc.block.ModBlocks;
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

/**
 * The contract a {@link CompressionPortBlock} needs from whichever multiblock controller seals the centre of its
 * structure: forward an energy buffer (POWER ports), an item handler (ITEM_IN / ITEM_OUT), or a BCE store, per port
 * kind. Three machines answer through it - the 5x5x5 Compression Manipulator and the 3x3x3 Decompression Manipulator
 * (both full {@link IMultiblockController}s, which extends this) and the 3x3x4 tier-1 generator
 * ({@link GeneratorCoreBlockEntity}, which deliberately does <em>not</em> - it has no GUI and no compression
 * semantics, only ports). The controller itself is the only party that knows what each port kind means on its
 * machine: a fuel sink, a seed insert, or the one BCE-out drain.
 *
 * <p>Resolution is positional: a shell cell walks {@link #CORE_SCAN_OFFSETS} - every cell within
 * {@link #MAX_SHELL_EXTENT}, nearest first - looking for a block entity implementing this interface. Ports cache the
 * found host's position exactly like they cached a controller before, and the {@link CompressionInterfaceBlock} now
 * resolves through this <em>same</em> call too, dispatching on what it gets back: a {@link IMultiblockController}
 * opens that machine's screen, a plain host like {@link GeneratorCoreBlockEntity} answers the click with a chat
 * status readout instead. One walk, one answer, so the panel can never disagree with the pipes around it.
 */
public interface IMultiblockPortHost {
    /**
     * The largest Chebyshev distance a shell cell can sit from its own core across every band in
     * {@link CompressionBand} - band 8's 19x19x19 compressor reaches {@code 9} blocks out. A lookup cannot know which
     * band built the shell it is standing in, so it scans this far but ordered outward, which keeps the shipped
     * band-1 answer cheap. The capability sweep at {@link #invalidatePortCaps} does <em>not</em> use this: a host
     * knows its own size, so it takes {@link #portShellExtent()} instead.
     */
    int MAX_SHELL_EXTENT = CompressionBand.maxShellExtent();

    /**
     * Every cell within {@link #MAX_SHELL_EXTENT} of a core, grouped by increasing Chebyshev radius so the nearest
     * cores are tested first and the core's own cell (radius 0) is tested absolutely first. Built once, statically:
     * a band-1 port resolves its host inside the leading {@code 5x5x5 = 125} offsets, exactly the cube the old
     * hardcoded radius-2 loop walked, while a bigger band just keeps walking outward.
     */
    BlockPos[] CORE_SCAN_OFFSETS = buildCoreScanOffsets();

    /**
     * Assembles {@link #CORE_SCAN_OFFSETS} ring by ring. Kept as a ring sweep rather than a sorted cube so the order
     * is obvious at a glance: all of radius {@code n} precedes all of radius {@code n + 1}.
     */
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
     * The Chebyshev radius of the shell this host owns, which is what the load-time capability sweep has to cover.
     * Every shipped machine is a band-1 structure, so that is the default; a band-2 core overrides it to return
     * {@code CompressionBand.BAND_2.shellExtent()}. Deliberately not {@link #MAX_SHELL_EXTENT}: the sweep is a cost
     * paid on every chunk load, and sweeping the whole table would invalidate thousands of positions per core.
     */
    default int portShellExtent() {
        return CompressionBand.BAND_1.shellExtent();
    }

    /**
     * Locate the host whose structure contains the shell cell at {@code from}, walking {@link #CORE_SCAN_OFFSETS}
     * outward from the cell itself until one of the three registered core blocks is found. Overlapping multiblocks
     * are not a supported build, so first-match is sufficient - and because the walk is nearest-first, first-match is
     * also the *correct* match when two structures are improperly touching. This is deliberately the <b>only</b> host
     * lookup in the mod: the GUI panel resolves through here as well rather than through a narrower "find a
     * controller" probe, because such a probe would skip straight over a nearer non-controller host (a generator
     * core) and bind the panel to a distant machine. The unloaded-cell guard matters more than it used to: the scan
     * is now wide enough to cross chunk borders routinely, and {@link Level#getBlockState} would otherwise generate
     * chunks just to answer a lookup.
     */
    @Nullable
    static IMultiblockPortHost findPortHost(Level level, BlockPos from) {
        for (BlockPos off : CORE_SCAN_OFFSETS) {
            BlockPos p = off.offset(from);
            if (!level.isLoaded(p)) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            if ((s.is(ModBlocks.CORE.get()) || s.is(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get())
                    || s.is(ModBlocks.GENERATOR_CORE.get()))
                    && level.getBlockEntity(p) instanceof IMultiblockPortHost host) {
                return host;
            }
        }
        return null;
    }

    /** Convenience for port entities that need the host's position (cache invalidation, messages). */
    static @Nullable BlockPos hostPos(@Nullable IMultiblockPortHost host) {
        return host instanceof BlockEntity be ? be.getBlockPos() : null;
    }

    /**
     * Collect every {@link GeneratorCoreBlockEntity} that claims the shell cell {@code from} as one of its POWER
     * ports - the co-owners of a POWER port standing on a wall two generator towers share. Unlike {@link
     * #findPortHost}, which stops at the single nearest host, this returns <em>all</em> claimers so the port can split
     * the FE a cable pushes into it evenly between them (see {@code MASTER_PLAN.md} D24). Only generator cores
     * participate: a compressor or decompressor POWER port is claimed by no generator core and so yields an empty
     * list, leaving those machines on the ordinary single-host path. Both co-owners of a seam sit within radius
     * {@code r <= } {@link #MAX_SHELL_EXTENT} of the port, so the shared {@link #CORE_SCAN_OFFSETS} sweep reaches them.
     */
    static List<IMultiblockPortHost> findGeneratorPowerHosts(Level level, BlockPos from) {
        List<IMultiblockPortHost> hosts = new ArrayList<>();
        for (BlockPos off : CORE_SCAN_OFFSETS) {
            BlockPos p = off.offset(from);
            if (!level.isLoaded(p)) {
                continue;
            }
            if (level.getBlockState(p).is(ModBlocks.GENERATOR_CORE.get())
                    && level.getBlockEntity(p) instanceof GeneratorCoreBlockEntity core
                    && core.claimsPowerPort(from)) {
                hosts.add(core);
            }
        }
        return hosts;
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
     * <p>The sweep is bounded by the caller's {@code extent} - the size of <em>that</em> host's shell, not the whole
     * table - so a bigger band invalidates proportionally more positions instead of a fixed little cube that silently
     * misses its outer ports.
     */
    static void invalidatePortCaps(Level level, BlockPos corePos, int extent) {
        for (int dx = -extent; dx <= extent; dx++) {
            for (int dy = -extent; dy <= extent; dy++) {
                for (int dz = -extent; dz <= extent; dz++) {
                    BlockPos p = corePos.offset(dx, dy, dz);
                    if (!level.isLoaded(p)) {
                        continue;
                    }
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
