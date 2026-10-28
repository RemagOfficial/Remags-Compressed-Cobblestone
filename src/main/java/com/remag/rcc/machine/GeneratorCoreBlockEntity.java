package com.remag.rcc.machine;

import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.BceNetwork;
import com.remag.rcc.energy.IBceStorage;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GeneratorCoreBlockEntity extends BlockEntity implements IMultiblockPortHost, IGeneratorArrayMember {
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_POOL = "bcePool";
    private static final String TAG_FORMED = "formed";
    private static final String TAG_MINT_COUNTDOWN = "mintCountdown";

    private static final int RESCAN_INTERVAL = CompressionConfig.GEN_MINT_PERIOD_TICKS;

    private int energy = 0;
    private double bcePool = Double.NEGATIVE_INFINITY;
    private boolean formed = false;
    private int mintCountdown = CompressionConfig.GEN_MINT_PERIOD_TICKS;

    private int rescanCooldown = 0;
    @Nullable
    private BlockPos bceOutPos = null;
    @Nullable
    private BceNetwork bus = null;
    private List<BlockPos> powerPorts = List.of();
    private final List<BlockPos> matchedPowerPorts = new ArrayList<>();

    private final IEnergyStorage energyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return GeneratorCoreBlockEntity.this.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return GeneratorCoreBlockEntity.this.energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return rung().energyCapacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    private final IBceStorage bceView = new IBceStorage() {
        @Override
        public double getBceStoredLog() {
            return GeneratorCoreBlockEntity.this.bcePool;
        }

        @Override
        public double getMaxBceLog() {
            return CompressionConfig.generatorLumpCeilingLog(lumpRateLog());
        }

        @Override
        public double receiveBce(double amountLog, boolean simulate) {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double extractBce(double amountLog, boolean simulate) {
            double pool = GeneratorCoreBlockEntity.this.bcePool;
            if (pool == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            double take = Math.min(amountLog, pool);
            if (!simulate) {
                GeneratorCoreBlockEntity.this.bcePool = CompressionConfig.log10subtract(pool, take);
                setChanged();
            }
            return take;
        }

        @Override
        public boolean canReceive() {
            return false;
        }

        @Override
        public boolean canExtract() {
            return true;
        }
    };

    public GeneratorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERATOR_CORE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null) {
            IMultiblockPortHost.invalidatePortCaps(this.level, this.worldPosition, this.portShellExtent());
        }
    }

    public CompressionBand band() {
        return getBlockState().getBlock() instanceof GeneratorCoreBlock core
                ? core.band() : CompressionBand.BAND_1;
    }

    public GeneratorCoreTier rung() {
        return new GeneratorCoreTier(band(), getBlockState().getValue(GeneratorCoreBlock.RUNG));
    }

    @Override
    public int portShellExtent() {
        return band().shellExtent();
    }

    @Override
    @Nullable
    public IItemHandler portItemHandler(PortType type) {
        return null;
    }

    @Override
    @Nullable
    public IBceStorage portBceHandler(PortType type) {
        return type == PortType.ITEM_OUT ? bceView : null;
    }

    @Override
    public IEnergyStorage getEnergy() {
        return energyView;
    }

    private int receiveEnergy(int maxReceive, boolean simulate) {
        int toStore = Math.min(rung().energyCapacity() - this.energy, Math.max(0, maxReceive));
        if (!simulate) {
            this.energy += toStore;
            setChanged();
        }
        return toStore;
    }

    private void drainEnergy(int amount) {
        this.energy = Math.max(0, this.energy - Math.max(0, amount));
    }

    @Override
    @Nullable
    public BigInteger arrayShareExponent() {
        if (!this.formed || this.energy < rung().energyPerTick()) {
            return null;
        }
        return rung().shareExponent();
    }

    @Override
    public CompressionBand arrayBand() {
        return rung().band();
    }

    private double lumpRateLog() {
        BceNetwork bus = this.bus;
        if (bus != null) {
            double aggregate = bus.arrayRateLog10PerSec();
            if (aggregate != Double.NEGATIVE_INFINITY) {
                return aggregate;
            }
        }
        return rung().rateLog10PerSec();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GeneratorCoreBlockEntity be) {
        if (--be.rescanCooldown <= 0) {
            be.rescanCooldown = RESCAN_INTERVAL;
            be.scanRegion();
            be.bus = be.bceOutPos == null ? null : BceNetwork.forGenerator(level, be.bceOutPos);
        }
        if (!be.formed) {
            return;
        }
        GeneratorCoreTier rung = be.rung();
        int cost = rung.energyPerTick();
        if (be.energy < cost) {
            return;
        }
        be.drainEnergy(cost);
        if (--be.mintCountdown > 0) {
            return;
        }
        be.mintCountdown = CompressionConfig.GEN_MINT_PERIOD_TICKS;
        be.mintLump();
    }

    private void mintLump() {
        BceNetwork bus = this.bus;
        boolean lead = bus == null || bus.isGeneratorLead(this.worldPosition);
        if (!lead || this.bcePool != Double.NEGATIVE_INFINITY) {
            return;
        }
        double lumpLog = lumpRateLog();
        if (lumpLog == Double.NEGATIVE_INFINITY) {
            return;
        }
        this.bcePool = lumpLog;
        this.setChanged();
    }

    public String contentsSummary() {
        GeneratorCoreTier rung = rung();
        BceNetwork bus = this.bus;
        int members = bus == null ? 1 : bus.arraySize();
        long wanted = bus == null ? 0L : bus.arrayFullOutputTowers();
        double total = lumpRateLog();
        String arrayText = bus == null
                ? "solo (no BCE bus on the output port)"
                : "array " + members + (wanted > 0L ? " / " + wanted + " towers" : " towers");
        return rung.describe() + " share, " + arrayText + "; bus total "
                + DisplayNames.bceMagnitude(total) + "/sec, " + leadWord(bus)
                + ", FE " + this.energy + " / " + rung.energyCapacity()
                + ", buffer " + DisplayNames.bceMagnitude(this.bcePool) + " / "
                + DisplayNames.bceMagnitude(bceView.getMaxBceLog()) + "; " + gateSummary(rung);
    }

    private String leadWord(@Nullable BceNetwork bus) {
        if (bus == null) {
            return "minting solo";
        }
        return bus.isGeneratorLead(this.worldPosition) ? "lead (mints the lump)" : "upkeep only";
    }

    private String gateSummary(GeneratorCoreTier rung) {
        if (!formed) {
            return "structure incomplete (needs 3x3x4 shell with a 1x1 air shaft directly above or below the "
                    + "core, 1+ POWER port, exactly 1 BCE-out port and 1 interface; towers may share a wall, and on a "
                    + "shared wall only frame/glass/POWER ports are allowed - a shared POWER port splits its input "
                    + "FE across every tower on that wall, while the BCE-out port and interface must stay private)";
        }
        if (this.energy < rung.energyPerTick()) {
            return "starved - needs " + String.format(java.util.Locale.US, "%,d", rung.energyPerTick())
                    + " FE in the buffer and that much per tick sustained upkeep";
        }
        if (this.bcePool != Double.NEGATIVE_INFINITY) {
            return "lump waiting to drain - attach an extract BCE pipe to the output port";
        }
        BceNetwork bus = this.bus;
        if (bus != null) {
            if (bus.isArrayClamped()) {
                return "array full - clamped at two band-max blocks";
            }
            if (!bus.isGeneratorLead(this.worldPosition)) {
                return "not the lead - this tower is upkeep only";
            }
        }
        return "minting - structure complete";
    }

    public void scanRegion() {
        if (level == null || level.isClientSide()) {
            return;
        }
        CompressionBand band = band();
        int r = band.generatorHalfWidth();
        int below = band.generatorLayersBelow();
        int above = band.generatorLayersAbove();
        List<BlockPos> oldPorts = this.powerPorts;
        this.bceOutPos = null;
        this.powerPorts = List.of();
        this.formed = matchesStructure(-below, above, below, r)
                || matchesStructure(-above, below, -below, r);
        if (!this.formed) {
            this.bceOutPos = null;
            this.powerPorts = List.of();
        }
        invalidateChangedPowerPorts(oldPorts, this.powerPorts);
    }

    private void invalidateChangedPowerPorts(List<BlockPos> oldPorts, List<BlockPos> newPorts) {
        if (oldPorts.equals(newPorts)) {
            return;
        }
        Set<BlockPos> changed = new HashSet<>(oldPorts);
        changed.addAll(newPorts);
        for (BlockPos port : changed) {
            level.invalidateCapabilities(port);
        }
    }

    public boolean claimsPowerPort(BlockPos cell) {
        return this.formed && this.powerPorts.contains(cell);
    }

    public List<BlockPos> claimedPowerPorts() {
        return this.powerPorts;
    }

    @Nullable
    private BlockPos matchedBceOutPos = null;

    private boolean matchesStructure(int dyMin, int dyMax, int shaftDy, int r) {
        int power = 0;
        int bceOut = 0;
        int interfaces = 0;
        this.matchedBceOutPos = null;
        this.matchedPowerPorts.clear();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = dyMin; dy <= dyMax; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    if (!level.isLoaded(p)) {
                        return false;
                    }
                    BlockState s = level.getBlockState(p);
                    if (dx == 0 && dz == 0 && dy == shaftDy) {
                        if (!s.isAir()) {
                            return false;
                        }
                        continue;
                    }
                    int boundary = (Math.abs(dx) == r ? 1 : 0)
                            + (Math.abs(dz) == r ? 1 : 0)
                            + (dy == dyMin || dy == dyMax ? 1 : 0);
                    boolean shared = isSharedCell(dx, dz, r);
                    if (boundary >= 2) {
                        if (s.is(ModBlocks.FRAME.get())) {
                        } else if (s.is(ModBlocks.PORT.get())) {
                            PortType type = s.getValue(CompressionPortBlock.PORT_TYPE);
                            if (shared && type != PortType.POWER) {
                                return false;
                            }
                            switch (type) {
                                case POWER -> {
                                    power++;
                                    this.matchedPowerPorts.add(p.immutable());
                                }
                                case ITEM_OUT -> {
                                    bceOut++;
                                    this.matchedBceOutPos = p.immutable();
                                }
                                case ITEM_IN -> {
                                    return false;
                                }
                                case SEED -> {
                                    return false;
                                }
                            }
                        } else {
                            return false;
                        }
                    } else if (boundary == 1) {
                        if (s.is(ModBlocks.GLASS.get())) {
                        } else if (s.is(ModBlocks.INTERFACE.get())) {
                            if (shared) {
                                return false;
                            }
                            interfaces++;
                        } else if (s.is(ModBlocks.PORT.get())) {
                            PortType type = s.getValue(CompressionPortBlock.PORT_TYPE);
                            if (shared && type != PortType.POWER) {
                                return false;
                            }
                            switch (type) {
                                case POWER -> {
                                    power++;
                                    this.matchedPowerPorts.add(p.immutable());
                                }
                                case ITEM_OUT -> {
                                    bceOut++;
                                    this.matchedBceOutPos = p.immutable();
                                }
                                case ITEM_IN -> {
                                    return false;
                                }
                                case SEED -> {
                                    return false;
                                }
                            }
                        } else {
                            return false;
                        }
                    } else if (!s.isAir()) {
                        return false;
                    }
                }
            }
        }
        boolean ok = power >= band().minPowerPorts()
                && bceOut == CompressionConfig.EXACT_GEN1_BCE_OUT_PORTS
                && interfaces >= CompressionConfig.MIN_INTERFACES
                && interfaces <= CompressionConfig.MAX_INTERFACES
                && this.matchedBceOutPos != null;
        if (ok) {
            this.bceOutPos = this.matchedBceOutPos;
            this.powerPorts = List.copyOf(this.matchedPowerPorts);
        }
        return ok;
    }

    private boolean isSharedCell(int dx, int dz, int r) {
        if (Math.abs(dx) == r && sameBandCoreAt(worldPosition.offset(Integer.signum(dx) * 2 * r, 0, 0))) {
            return true;
        }
        return Math.abs(dz) == r && sameBandCoreAt(worldPosition.offset(0, 0, Integer.signum(dz) * 2 * r));
    }

    private boolean sameBandCoreAt(BlockPos p) {
        if (!level.isLoaded(p)) {
            return false;
        }
        BlockState s = level.getBlockState(p);
        return s.getBlock() instanceof GeneratorCoreBlock core
                && core.band() == this.rung().band();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        builder.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY
                .with(GeneratorCoreBlock.RUNG, getBlockState().getValue(GeneratorCoreBlock.RUNG)));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_ENERGY, energy);
        tag.putDouble(TAG_POOL, bcePool);
        tag.putBoolean(TAG_FORMED, formed);
        tag.putInt(TAG_MINT_COUNTDOWN, mintCountdown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.energy = tag.getInt(TAG_ENERGY);
        this.bcePool = tag.contains(TAG_POOL) ? tag.getDouble(TAG_POOL) : Double.NEGATIVE_INFINITY;
        this.formed = tag.getBoolean(TAG_FORMED);
        this.mintCountdown = tag.contains(TAG_MINT_COUNTDOWN)
                ? tag.getInt(TAG_MINT_COUNTDOWN)
                : CompressionConfig.GEN_MINT_PERIOD_TICKS;
    }
}
