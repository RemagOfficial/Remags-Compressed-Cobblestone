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

public final class BceNetwork {
    private record Endpoint(BlockPos store, Direction side, double capLog) {
    }

    private record GeneratorMember(BlockPos store, BigInteger share, CompressionBand band) {
    }

    private final Level level;
    private final Set<BlockPos> pipes = new HashSet<>();
    private final List<Endpoint> sources = new ArrayList<>();
    private final List<Endpoint> sinks = new ArrayList<>();
    private final List<GeneratorMember> members = new ArrayList<>();

    private BigInteger arrayRawSum = BigInteger.ZERO;
    private BigInteger arrayExponent = BigInteger.ZERO;
    @Nullable
    private CompressionBand arrayBand;
    @Nullable
    private BlockPos arrayLead;

    private boolean stale;
    private long endpointsBuiltAt = Long.MIN_VALUE;
    private long lastTransferGameTime = Long.MIN_VALUE;

    private double lastMovedLog = Double.NEGATIVE_INFINITY;
    private int lastSkippedLegs;

    private BceNetwork(Level level) {
        this.level = level;
    }

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
        for (BlockPos pos : network.pipes) {
            if (level.getBlockEntity(pos) instanceof BcePipeBlockEntity member) {
                member.setNetwork(network);
            }
        }
        return network;
    }

    public void markStale() {
        this.stale = true;
        this.endpointsBuiltAt = Long.MIN_VALUE;
    }

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
                continue;
            }
            BcePipeTier tier = state.getValue(BcePipeBlock.TIER);
            for (Direction dir : Direction.values()) {
                BcePipeMode mode = state.getValue(BcePipeBlock.END_BY_DIRECTION.get(dir));
                if (!mode.isLinked()) {
                    continue;
                }
                BlockPos store = pos.relative(dir);
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
        this.sources.removeIf(source -> touches(this.sinks, source.store()));
        this.sinks.removeIf(sink -> touches(this.sources, sink.store()));
        this.sources.sort(Comparator.comparingLong(endpoint -> endpoint.store().asLong()));
        this.sinks.sort(Comparator.comparingLong(endpoint -> endpoint.store().asLong()));
        rebuildArray();
        this.endpointsBuiltAt = now;
    }

    private void addMember(BlockPos store) {
        BlockEntity be = this.level.getBlockEntity(store);
        if (be instanceof CompressionPortBlockEntity port && port.resolveHost() instanceof BlockEntity core) {
            be = core;
        }
        if (!(be instanceof IGeneratorArrayMember member)) {
            return;
        }
        BigInteger share = member.arrayShareExponent();
        if (share == null) {
            return;
        }
        BlockPos host = ((BlockEntity) member).getBlockPos();
        for (GeneratorMember existing : this.members) {
            if (existing.store().equals(host)) {
                return;
            }
        }
        this.members.add(new GeneratorMember(host, share, member.arrayBand()));
    }

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
        this.arrayExponent = band == null ? BigInteger.ZERO : sum.min(arrayCeiling(band));
        this.arrayLead = this.members.isEmpty() ? null : this.members.get(0).store();
    }

    private static BigInteger arrayCeiling(CompressionBand band) {
        return band.maxOutputTier().shiftLeft(1);
    }

    private void ensureEndpoints() {
        refreshEndpoints(this.level.getGameTime());
    }

    public int arraySize() {
        ensureEndpoints();
        return this.members.size();
    }

    public BigInteger arrayExponent() {
        ensureEndpoints();
        return this.arrayExponent;
    }

    public boolean isArrayClamped() {
        ensureEndpoints();
        return this.arrayBand != null && this.arrayRawSum.compareTo(arrayCeiling(this.arrayBand)) > 0;
    }

    public long arrayFullOutputTowers() {
        ensureEndpoints();
        return this.arrayBand == null ? 0L : this.arrayBand.arrayFullOutputTowers();
    }

    public double arrayRateLog10PerSec() {
        ensureEndpoints();
        return this.members.isEmpty() ? Double.NEGATIVE_INFINITY : CompressionConfig.bceMassLog(this.arrayExponent);
    }

    public boolean isGeneratorLead(BlockPos pos) {
        ensureEndpoints();
        return pos.equals(this.arrayLead);
    }

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
                continue;
            }
            double remaining = Math.min(pool, source.capLog());

            for (Endpoint sink : this.sinks) {
                if (remaining == Double.NEGATIVE_INFINITY) {
                    break;
                }
                IBceStorage to = resolve(sink);
                if (to == null || !to.canReceive()) {
                    continue;
                }
                double accepted = to.receiveBce(remaining, false);
                if (accepted == Double.NEGATIVE_INFINITY) {
                    this.lastSkippedLegs++;
                    continue;
                }
                if (from.extractBce(accepted, false) == Double.NEGATIVE_INFINITY) {
                    to.extractBce(accepted, false);
                    continue;
                }
                remaining = CompressionConfig.log10subtract(remaining, accepted);
                this.lastMovedLog = CompressionConfig.log10add(this.lastMovedLog, accepted);
            }
        }
    }

    private IBceStorage resolve(Endpoint endpoint) {
        return this.level.getCapability(ModCapabilities.BCE, endpoint.store(), endpoint.side());
    }

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
