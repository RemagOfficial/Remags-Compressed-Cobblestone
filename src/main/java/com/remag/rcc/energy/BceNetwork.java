package com.remag.rcc.energy;

import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeBlockEntity;
import com.remag.rcc.machine.BcePipeMode;
import com.remag.rcc.machine.BcePipeTier;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;
import com.remag.rcc.registry.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One connected run of {@link BcePipeBlock}s, acting as a shared BCE bus: <em>every</em>
 * {@link BcePipeMode#EXTRACT} end pulls from the stores it touches and <em>every</em>
 * {@link BcePipeMode#INSERT} end pushes to the stores it touches, so a decompressor at one end of a 200-block run
 * feeds a tank at the other with no per-hop delay. Modes belong to individual ends rather than to a pipe, so one
 * segment can drain two stores and fill a third at the same time. The pipes themselves store nothing - every transfer
 * runs straight from source to sink - so there is no in-flight amount to persist, sync, or lose when a chunk unloads.
 *
 * <p><b>Partial fills.</b> A source offers up to its tier's absolute per-transfer ceiling ({@code min(pool, ceiling)})
 * to each sink in turn and the sink keeps only what fits, so an undersized or near-full store is filled to its ceiling
 * instead of refusing the transfer outright - the remainder simply stays in the source until a bigger sink appears or
 * the next tick. Every extract is exactly the amount its sink accepted, so a store that took nothing never loses anything.
 *
 * <p><b>The precision limit.</b> All amounts are log10 BCE (see {@link IBceStorage}), and a {@code double} at
 * magnitude ~954 has an ulp near 1e-13, so pulling a slice more than ~13 orders smaller than a huge pool leaves that
 * pool's stored figure unchanged - a sliver is effectively created, but it is bounded by the receiving sink's own
 * capacity and is invisible against the source's magnitude, so the trade is accepted rather than gating the leg. The
 * old {@link CompressionConfig#BCE_TRANSFER_FLOOR_LOG} transfer gate (which skipped any leg outside it of either
 * endpoint) is gone; that constant now only sizes a store's headroom. Refused legs are still counted in
 * {@link #statusReport()} when a pipe is sneak-right-clicked.
 *
 * <p><b>Laziness.</b> Topology changes never rebuild eagerly: {@code BcePipeBlock#neighborChanged} only marks the
 * network {@link #markStale() stale}, and the next ticking pipe re-floods the run from itself (see
 * {@link #networkFor}). A broken pipe therefore transparently becomes two networks, and a pipe placed between two
 * runs transparently merges them - no explicit merge/split code exists.
 */
public final class BceNetwork {
    /** A store one pipe end touches, plus the absolute per-transfer ceiling (log10 BCE) of the extract tier that reached it. */
    private record Endpoint(BlockPos store, Direction side, double capLog) {
    }

    private final Level level;
    private final Set<BlockPos> pipes = new HashSet<>();
    private final List<Endpoint> sources = new ArrayList<>();
    private final List<Endpoint> sinks = new ArrayList<>();

    private boolean stale;
    private long endpointsBuiltAt = Long.MIN_VALUE;
    private long lastTransferGameTime = Long.MIN_VALUE;

    /** Last-transfer stats, shown by the sneak-click status readout. */
    private double lastMovedLog = Double.NEGATIVE_INFINITY;
    private int lastSkippedLegs;

    private BceNetwork(Level level) {
        this.level = level;
    }

    // --- Lifecycle ---------------------------------------------------------------------------

    /**
     * The network {@code pipe} belongs to, re-flooding the run when the cached one is missing or stale. Every pipe
     * the flood reaches is re-pointed at the returned network, so a run shares exactly one object (and thus
     * transfers once per tick no matter how many of its pipes tick), and a merge is just a flood spanning both old
     * runs.
     */
    public static BceNetwork networkFor(BcePipeBlockEntity pipe) {
        Level level = pipe.getLevel();
        if (level == null) {
            return null;
        }
        BceNetwork cached = pipe.getNetwork();
        if (cached != null && !cached.stale && cached.pipes.contains(pipe.getBlockPos())) {
            return cached;
        }
        BceNetwork network = new BceNetwork(level);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(pipe.getBlockPos().immutable());
        while (!queue.isEmpty()) {
            BlockPos here = queue.removeFirst();
            if (!network.pipes.add(here)) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = here.relative(dir);
                if (!network.pipes.contains(next) && level.getBlockEntity(next) instanceof BcePipeBlockEntity) {
                    queue.add(next.immutable());
                }
            }
        }
        // Hand the fresh network to every pipe we found, replacing whatever they had.
        for (BlockPos pos : network.pipes) {
            if (level.getBlockEntity(pos) instanceof BcePipeBlockEntity member) {
                member.setNetwork(network);
            }
        }
        return network;
    }

    /** Marks the topology and endpoint cache suspect; the rebuild happens lazily on the next tick. */
    public void markStale() {
        this.stale = true;
        this.endpointsBuiltAt = Long.MIN_VALUE;
    }

    /** Driven by every pipe BE in the run; only the first caller in a given tick actually moves anything. */
    public void tick() {
        if (this.level.isClientSide()) {
            return;
        }
        long now = this.level.getGameTime();
        if (this.lastTransferGameTime == now) {
            return;
        }
        this.lastTransferGameTime = now;
        refreshEndpoints(now);
        if (this.sources.isEmpty() || this.sinks.isEmpty()) {
            this.lastMovedLog = Double.NEGATIVE_INFINITY;
            this.lastSkippedLegs = 0;
            return;
        }
        transfer();
    }

    // --- Endpoint discovery ------------------------------------------------------------------

    /** Re-scan the stores touching this run unless the cache is still fresh (see the TTL constant). */
    private void refreshEndpoints(long now) {
        boolean fresh = !this.stale
                && this.endpointsBuiltAt != Long.MIN_VALUE
                && now - this.endpointsBuiltAt <= CompressionConfig.BCE_NETWORK_ENDPOINT_TTL_TICKS;
        if (fresh) {
            return;
        }
        this.stale = false;
        this.sources.clear();
        this.sinks.clear();
        for (BlockPos pos : this.pipes) {
            BlockState state = this.level.getBlockState(pos);
            if (!(state.getBlock() instanceof BcePipeBlock)) {
                continue; // the neighbouring chunk may be unloaded, or the pipe was just broken
            }
            BcePipeTier tier = state.getValue(BcePipeBlock.TIER);
            for (Direction dir : Direction.values()) {
                // The direction lives on this one end, not on the block: a segment's north end can extract while its
                // west end inserts, and an unlinked (off) end is not an endpoint of anything.
                BcePipeMode mode = state.getValue(BcePipeBlock.END_BY_DIRECTION.get(dir));
                if (!mode.isLinked()) {
                    continue;
                }
                BlockPos store = pos.relative(dir);
                // The capability context is the face of the *store* being approached, hence the opposite.
                Direction face = dir.getOpposite();
                IBceStorage storage = this.level.getCapability(ModCapabilities.BCE, store, face);
                if (storage == null) {
                    continue;
                }
                if (mode == BcePipeMode.EXTRACT && storage.canExtract()) {
                    offerBest(this.sources, store, face, tier.maxTransferLog());
                } else if (mode == BcePipeMode.INSERT && storage.canReceive()) {
                    offerBest(this.sinks, store, face, 0.0D);
                }
            }
        }
        // A store touched by both an extract and an insert end would be pumped on one tick and refilled on the next
        // for no net effect; drop it from both lists so the bus stays one-directional.
        this.sources.removeIf(source -> touches(this.sinks, source.store()));
        this.sinks.removeIf(sink -> touches(this.sources, sink.store()));
        // Deterministic order regardless of HashSet iteration, so ties always fill the same sink first.
        this.sources.sort(Comparator.comparingLong(endpoint -> endpoint.store().asLong()));
        this.sinks.sort(Comparator.comparingLong(endpoint -> endpoint.store().asLong()));
        this.endpointsBuiltAt = now;
    }

    /** Keeps the most generous (largest ceiling) endpoint when several pipes reach the same store. */
    private static void offerBest(List<Endpoint> list, BlockPos store, Direction face, double capLog) {
        for (int i = 0; i < list.size(); i++) {
            Endpoint existing = list.get(i);
            if (existing.store().equals(store)) {
                if (capLog > existing.capLog()) {
                    list.set(i, new Endpoint(store, face, capLog));
                }
                return;
            }
        }
        list.add(new Endpoint(store, face, capLog));
    }

    private static boolean touches(List<Endpoint> list, BlockPos store) {
        for (Endpoint endpoint : list) {
            if (endpoint.store().equals(store)) {
                return true;
            }
        }
        return false;
    }

    // --- The bus tick ------------------------------------------------------------------------

    /**
     * Drain each source into the sinks greedily, filling every store as much as it will take. The pipe's throughput is
     * {@code min(pool, tier ceiling)} - an absolute per-transfer amount, not a fraction - and a sink that can only take
     * a part of it takes just that part, while the rest carries on down the list until the slice runs out. That is what
     * lets a full tank feed a smaller store at all instead of refusing the mismatched pair.
     */
    private void transfer() {
        this.lastMovedLog = Double.NEGATIVE_INFINITY;
        this.lastSkippedLegs = 0;

        for (Endpoint source : this.sources) {
            IBceStorage from = resolve(source);
            if (from == null || !from.canExtract()) {
                continue;
            }
            double pool = from.getBceStoredLog();
            if (pool == Double.NEGATIVE_INFINITY) {
                continue; // empty, or its block entity went away between discovery and now
            }
            // The slice this tier may move this transfer: never more than its absolute ceiling, never more than the pool.
            double remaining = Math.min(pool, source.capLog());

            for (Endpoint sink : this.sinks) {
                if (remaining == Double.NEGATIVE_INFINITY) {
                    break; // this tier's slice is spent; the rest of the run waits for the next tick
                }
                IBceStorage to = resolve(sink);
                if (to == null || !to.canReceive()) {
                    continue;
                }
                // Offer everything the pipe still has; the sink keeps what fits and reports how much that was, so an
                // under-sized store fills exactly to its ceiling rather than bouncing the whole transfer.
                double accepted = to.receiveBce(remaining, false);
                if (accepted == Double.NEGATIVE_INFINITY) {
                    this.lastSkippedLegs++; // sink already full
                    continue;
                }
                // Take back out of the source precisely what the sink took, so a store that accepted nothing is never
                // drained and the pool only ever loses what another store gained.
                if (from.extractBce(accepted, false) == Double.NEGATIVE_INFINITY) {
                    to.extractBce(accepted, false); // source ran dry: undo the fill so nothing materialises
                    continue;
                }
                remaining = CompressionConfig.log10subtract(remaining, accepted);
                this.lastMovedLog = CompressionConfig.log10add(this.lastMovedLog, accepted);
            }
        }
    }

    /** Re-resolves a store's capability live; a cached view could outlive its block entity. */
    private IBceStorage resolve(Endpoint endpoint) {
        return this.level.getCapability(ModCapabilities.BCE, endpoint.store(), endpoint.side());
    }

    // --- Readouts ----------------------------------------------------------------------------

    /**
     * What this run sees, shown when a pipe is sneak-right-clicked: endpoint counts, the volume moved by the last
     * transfer, and how many legs were skipped because the sink was already full (the usual reason a line appears to do
     * nothing).
     */
    public Component statusReport() {
        return Component.translatable("chat.rcc.bce_network.status",
                this.sources.size(),
                this.sinks.size(),
                DisplayNames.bceMagnitude(this.lastMovedLog),
                this.lastSkippedLegs);
    }

    public int pipeCount() {
        return this.pipes.size();
    }

    public List<BlockPos> sourcePositions() {
        List<BlockPos> positions = new ArrayList<>(this.sources.size());
        for (Endpoint endpoint : this.sources) {
            positions.add(endpoint.store());
        }
        return positions;
    }

    public List<BlockPos> sinkPositions() {
        List<BlockPos> positions = new ArrayList<>(this.sinks.size());
        for (Endpoint endpoint : this.sinks) {
            positions.add(endpoint.store());
        }
        return positions;
    }
}
