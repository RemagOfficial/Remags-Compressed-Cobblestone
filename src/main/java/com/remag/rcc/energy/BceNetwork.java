package com.remag.rcc.energy;

import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeBlockEntity;
import com.remag.rcc.machine.BcePipeMode;
import com.remag.rcc.machine.BcePipeTier;
import com.remag.rcc.machine.CompressionPortBlockEntity;
import com.remag.rcc.machine.IGeneratorArrayMember;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;
import com.remag.rcc.registry.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
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
 * <p><b>The precision limit, and the array rule it forces.</b> All amounts are log10 BCE (see {@link IBceStorage}), and
 * a {@code double} only resolves changes of about {@code 2.2e-16} <em>relative</em> to its magnitude: the ulp of a
 * band-2 rate figure is {@code 8192} and a band-8 one is {@code 9.7e83}, while {@link CompressionConfig#log10add} can
 * never move a pool by more than {@code log10(2) = 0.301} at a time. So a pool <em>cannot be banked</em> at those
 * magnitudes - one second of minting and a year of it are the same {@code double} - and {@code N} generators joined
 * that way would be indistinguishable from one. The bus therefore adds generators in <b>exponent</b> space instead:
 * every {@link IGeneratorArrayMember} draining into this run contributes its rung's {@link
 * CompressionBand#shareOf share} exponent, the bus sums those {@link BigInteger} exponents, clamps the total at the
 * highest band present's ceiling, and the one machine elected as {@link #isGeneratorLead array lead} mints the whole
 * {@code 9^sum} as a single lump per second. That is the {@code MASTER_PLAN.md} D23 rule, and it is why the mint is one
 * lump rather than twenty slices: a slice is the quantity this number system cannot represent.
 *
 * <p>Refused legs are still counted in {@link #statusReport()} when a pipe is sneak-right-clicked.
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

    /** One producing generator on this run: where its mint lives, and the exponent it contributes to the array. */
    private record GeneratorMember(BlockPos store, BigInteger share, CompressionBand band) {
    }

    private final Level level;
    private final Set<BlockPos> pipes = new HashSet<>();
    private final List<Endpoint> sources = new ArrayList<>();
    private final List<Endpoint> sinks = new ArrayList<>();
    /** The generators draining into this run, position-sorted so {@link #arrayLead} is deterministic. */
    private final List<GeneratorMember> members = new ArrayList<>();

    /** Sum of the members' share exponents, before the band clamp - only the readout needs the unclamped figure. */
    private BigInteger arrayRawSum = BigInteger.ZERO;
    /** Sum of the members' share exponents, clamped at the highest band present's ceiling: the lump size this bus mints. */
    private BigInteger arrayExponent = BigInteger.ZERO;
    /** The highest band among the members, whose {@link CompressionBand#maxOutputTier()} is the clamp. */
    @Nullable
    private CompressionBand arrayBand;
    /** The member elected to mint the array's lump, or {@code null} when the run has no generator on it. */
    @Nullable
    private BlockPos arrayLead;

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
        this.members.clear();
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
                    addMember(store);
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
        rebuildArray();
        this.endpointsBuiltAt = now;
    }

    // --- Generator array (D23) -----------------------------------------------------------------

    /**
     * Record the generator behind an extract end, if that store is one. A single-block generator answers directly;
     * a tower's extract end is attached to its {@link CompressionPortBlockEntity BCE-out port}, whose host resolution
     * is already cached on the port, so this is a block-entity read plus (towers only) one cached lookup - never a
     * fresh shell scan.
     */
    private void addMember(BlockPos store) {
        BlockEntity be = this.level.getBlockEntity(store);
        if (be instanceof CompressionPortBlockEntity port && port.resolveHost() instanceof BlockEntity core) {
            be = core;
        }
        if (!(be instanceof IGeneratorArrayMember member)) {
            return;
        }
        // null = "placed but not producing" (incomplete shell, starved, or an item-output rung), which must not count.
        BigInteger share = member.arrayShareExponent();
        if (share == null) {
            return;
        }
        BlockPos host = ((BlockEntity) member).getBlockPos();
        for (GeneratorMember existing : this.members) {
            if (existing.store().equals(host)) {
                return; // the same generator reached by two pipe ends still contributes once
            }
        }
        this.members.add(new GeneratorMember(host, share, member.arrayBand()));
    }

    /**
     * Sum the members' share exponents exactly, clamp the total at the highest band present's ceiling, and elect the
     * position-first member as the lead that will mint the lump. Called on every endpoint rebuild, so a generator
     * broken or starved mid-run leaves the array at the next refresh.
     */
    private void rebuildArray() {
        this.members.sort(Comparator.comparingLong(member -> member.store().asLong()));
        BigInteger sum = BigInteger.ZERO;
        CompressionBand band = null;
        for (GeneratorMember member : this.members) {
            sum = sum.add(member.share());
            if (band == null || member.band().index() > band.index()) {
                band = member.band();
            }
        }
        this.arrayRawSum = sum;
        this.arrayBand = band;
        this.arrayExponent = band == null ? BigInteger.ZERO : sum.min(band.maxOutputTier());
        this.arrayLead = this.members.isEmpty() ? null : this.members.get(0).store();
    }

    /** Rebuilds the endpoint caches if their TTL has run out, so a caller asking about the array never reads a stale one. */
    private void ensureEndpoints() {
        refreshEndpoints(this.level.getGameTime());
    }

    /** How many producing generators drain into this run. */
    public int arraySize() {
        ensureEndpoints();
        return this.members.size();
    }

    /**
     * The clamped aggregate exponent this bus mints from: {@code 9^arrayExponent()} is the lump the lead deposits each
     * {@link CompressionConfig#GEN_MINT_PERIOD_TICKS}. {@link BigInteger#ZERO} when no generator is on the run.
     */
    public BigInteger arrayExponent() {
        ensureEndpoints();
        return this.arrayExponent;
    }

    /** The share sum before the clamp, so a readout can say "array exceeds this band's ceiling" instead of lying. */
    public boolean isArrayClamped() {
        ensureEndpoints();
        return this.arrayBand != null && this.arrayRawSum.compareTo(this.arrayBand.maxOutputTier()) > 0;
    }

    /** Top-rung towers the array would need to mint a whole band-max block per second, or 0 with no generator on it. */
    public long arrayFullOutputTowers() {
        ensureEndpoints();
        return this.arrayBand == null ? 0L : this.arrayBand.arrayFullOutputTowers();
    }

    /** log10 BCE per second of the whole array, or {@link Double#NEGATIVE_INFINITY} for an empty bus. */
    public double arrayRateLog10PerSec() {
        ensureEndpoints();
        return this.members.isEmpty() ? Double.NEGATIVE_INFINITY : CompressionConfig.bceMassLog(this.arrayExponent);
    }

    /** The one member of this array that deposits the lump: the position-first producing generator. */
    public boolean isGeneratorLead(BlockPos pos) {
        ensureEndpoints();
        return pos.equals(this.arrayLead);
    }

    /**
     * The bus a generator at {@code store} drains into: the first pipe with an {@link BcePipeMode#EXTRACT} end pointing
     * back at it. For a tower {@code store} is the position of its single BCE-out <em>port</em> (the core itself is
     * sealed inside the shell and touches no pipe); for a single-block generator it is the generator's own position.
     * {@code null} means the machine is not on a bus at all, and mints its own share solo.
     */
    @Nullable
    public static BceNetwork forGenerator(Level level, BlockPos store) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(store.relative(dir)) instanceof BcePipeBlockEntity pipe) {
                BcePipeMode end = pipe.getBlockState().getValue(BcePipeBlock.END_BY_DIRECTION.get(dir.getOpposite()));
                if (end == BcePipeMode.EXTRACT) {
                    return networkFor(pipe);
                }
            }
        }
        return null;
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
     * transfer, how many legs were skipped because the sink was already full (the usual reason a line appears to do
     * nothing), and the generator array draining into it - how many producing machines and the {@code 9^sum} lump rate
     * their share exponents add up to (see {@link #arrayRateLog10PerSec}).
     */
    public Component statusReport() {
        return Component.translatable("chat.rcc.bce_network.status",
                this.sources.size(),
                this.sinks.size(),
                DisplayNames.bceMagnitude(this.lastMovedLog),
                this.lastSkippedLegs,
                this.arraySize(),
                DisplayNames.bceMagnitude(this.arrayRateLog10PerSec()));
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
