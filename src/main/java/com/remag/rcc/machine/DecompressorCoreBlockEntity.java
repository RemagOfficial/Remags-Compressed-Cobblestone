package com.remag.rcc.machine;

import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.IBceStorage;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.menu.CompressionCoreMenu;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public class DecompressorCoreBlockEntity extends BlockEntity
        implements IMultiblockController, IDecompressTarget {
    private static final String TAG_SEED = "seed";
    private static final String TAG_OUTPUT = "output";
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_BCE_POOL = "bcePool";
    private static final String TAG_GOAL = "goalTier";
    private static final String TAG_PROGRESS = "progress";
    private static final String TAG_FORMED = "formed";

    private static final int RESCAN_INTERVAL = 20;

    private final ItemStackHandler seed = new ItemStackHandler(1);
    private final ItemStackHandler output = new ItemStackHandler(1);

    private int energy = 0;
    private final IEnergyStorage energyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return DecompressorCoreBlockEntity.this.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return DecompressorCoreBlockEntity.this.energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return CompressionConfig.AUTO_ENERGY_CAPACITY;
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

    private double bcePool = Double.NEGATIVE_INFINITY;
    private BigInteger goalTier = BigInteger.ZERO;
    private int progress = 0;
    private boolean formed = false;

    private int rescanCooldown = 0;

    private final IBceStorage bceView = new IBceStorage() {
        @Override
        public double getBceStoredLog() {
            return DecompressorCoreBlockEntity.this.bcePool;
        }

        @Override
        public double getMaxBceLog() {
            return CompressionConfig.DECOMP_BCE_CAPACITY_LOG;
        }

        @Override
        public double receiveBce(double amountLog, boolean simulate) {
            if (!canReceive() || amountLog == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            double result = CompressionConfig.log10add(DecompressorCoreBlockEntity.this.bcePool, amountLog);
            if (result > CompressionConfig.DECOMP_BCE_CAPACITY_LOG) {
                return Double.NEGATIVE_INFINITY;
            }
            if (!simulate) {
                DecompressorCoreBlockEntity.this.bcePool = result;
                setChanged();
            }
            return amountLog;
        }

        @Override
        public double extractBce(double amountLog, boolean simulate) {
            if (!canExtract()) {
                return Double.NEGATIVE_INFINITY;
            }
            double pool = DecompressorCoreBlockEntity.this.bcePool;
            if (pool == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            double take = Math.min(amountLog, pool);
            if (!simulate) {
                DecompressorCoreBlockEntity.this.bcePool = CompressionConfig.log10subtract(pool, take);
                setChanged();
            }
            return take;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        @Override
        public boolean canExtract() {
            return true;
        }
    };

    public DecompressorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSION_DECOMPRESSOR_CORE.get(), pos, state);
    }

    public CompressionBand band() {
        return getBlockState().getBlock() instanceof DecompressorCoreBlock core
                ? core.band() : CompressionBand.BAND_1;
    }

    @Override
    public int portShellExtent() {
        return band().shellExtent();
    }

    @Override
    public int guiShellRadius() {
        return band().shellExtent();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null) {
            IMultiblockPortHost.invalidatePortCaps(this.level, this.worldPosition, this.portShellExtent());
        }
    }

    @Override
    public ItemStackHandler getSeed() {
        return seed;
    }

    @Override
    public ItemStackHandler getOutput() {
        return output;
    }

    @Override
    public IEnergyStorage getEnergy() {
        return energyView;
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public int getProgressMax() {
        return CompressionConfig.DECOMP_MULTIBLOCK_TICKS_PER_STEP;
    }

    @Override
    public double getFuelPool() {
        return bcePool;
    }

    @Override
    public double getRequiredFuel() {
        return CompressionConfig.DECOMP_BCE_DISPLAY_LOG;
    }

    @Override
    public boolean isFormed() {
        return formed;
    }

    @Override
    public void acceptFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        double massLog;
        if (stack.is(ModItems.COMPRESSED_COBBLESTONE.get())) {
            BigInteger tier = stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
            massLog = CompressionConfig.bceMassLog(tier);
        } else {
            massLog = 0.0;
        }
        massLog += Math.log10(stack.getCount());
        this.bceView.receiveBce(massLog, false);
        setChanged();
    }

    @Override
    public BigInteger getGoalTier() {
        return goalTier;
    }

    @Override
    public void setGoalTier(BigInteger goal) {
        this.goalTier = (goal == null || goal.signum() < 0) ? BigInteger.ZERO : goal;
        setChanged();
    }

    @Override
    public String goalTierString() {
        return goalTier.toString();
    }

    @Override
    @Nullable
    public IItemHandler portItemHandler(PortType type) {
        return switch (type) {
            case ITEM_IN -> new SeedInsertHandler(getSeed());
            case SEED -> new SeedInsertHandler(getSeed());
            case ITEM_OUT -> output;
            case POWER -> null;
        };
    }

    @Override
    @Nullable
    public IBceStorage portBceHandler(PortType type) {
        return type == PortType.POWER ? null : bceView;
    }

    private int receiveEnergy(int maxReceive, boolean simulate) {
        int toStore = Math.min(CompressionConfig.AUTO_ENERGY_CAPACITY - this.energy, Math.max(0, maxReceive));
        if (!simulate) {
            this.energy += toStore;
            setChanged();
        }
        return toStore;
    }

    private void drainEnergy(int amount) {
        this.energy = Math.max(0, this.energy - Math.max(0, amount));
    }

    @Nullable
    private BigInteger seedTier() {
        ItemStack stack = seed.getStackInSlot(0);
        if (stack.isEmpty() || !stack.is(ModItems.COMPRESSED_COBBLESTONE.get())) {
            return null;
        }
        return stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
    }

    private double releaseMassLog() {
        BigInteger t = seedTier();
        if (t == null || t.signum() <= 0) {
            return Double.NEGATIVE_INFINITY;
        }
        int count = seed.getStackInSlot(0).getCount();
        double perUnit = CompressionConfig.requiredStepMassLog(t.subtract(BigInteger.ONE));
        return count > 1 ? perUnit + Math.log10(count) : perUnit;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DecompressorCoreBlockEntity be) {
        if (--be.rescanCooldown <= 0) {
            be.rescanCooldown = RESCAN_INTERVAL;
            be.scanRegion();
        }
        if (be.progress > 0) {
            be.drainStepEnergy();
            if (++be.progress >= CompressionConfig.DECOMP_MULTIBLOCK_TICKS_PER_STEP) {
                be.completeStep();
            } else {
                be.setChanged();
            }
        } else if (be.canStep()) {
            be.progress = 1;
            be.drainStepEnergy();
            if (be.progress >= CompressionConfig.DECOMP_MULTIBLOCK_TICKS_PER_STEP) {
                be.completeStep();
            }
            be.setChanged();
        }
    }

    private void drainStepEnergy() {
        int perTick = Math.max(1,
                CompressionConfig.DECOMP_MULTIBLOCK_ENERGY_PER_STEP / CompressionConfig.DECOMP_MULTIBLOCK_TICKS_PER_STEP);
        drainEnergy(Math.min(perTick, this.energy));
    }

    private boolean canStep() {
        if (!formed || progress > 0) {
            return false;
        }
        BigInteger t = seedTier();
        if (t == null || t.compareTo(goalTier) <= 0) {
            return false;
        }
        if (energy < CompressionConfig.DECOMP_MULTIBLOCK_ENERGY_PER_STEP) {
            return false;
        }
        double release = releaseMassLog();
        if (!(CompressionConfig.log10add(bcePool, release) <= CompressionConfig.DECOMP_BCE_CAPACITY_LOG)) {
            return false;
        }
        if (t.subtract(BigInteger.ONE).compareTo(goalTier) <= 0) {
            ItemStack probe = stackForTier(t.subtract(BigInteger.ONE));
            probe.setCount(seed.getStackInSlot(0).getCount());
            if (!output.insertItem(0, probe, true).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void completeStep() {
        BigInteger t = seedTier();
        this.progress = 0;
        if (t == null || t.compareTo(goalTier) <= 0) {
            setChanged();
            return;
        }
        int seedCount = this.seed.getStackInSlot(0).getCount();
        double release = CompressionConfig.requiredStepMassLog(t.subtract(BigInteger.ONE))
                + (seedCount > 1 ? Math.log10(seedCount) : 0.0);
        this.bcePool = CompressionConfig.log10add(this.bcePool, release);

        BigInteger next = t.subtract(BigInteger.ONE);
        ItemStack result = stackForTier(next);
        result.setCount(seedCount);
        this.seed.extractItem(0, seedCount, false);
        if (next.compareTo(goalTier) > 0) {
            this.seed.insertItem(0, result, false);
        } else {
            this.output.insertItem(0, result, false);
        }
        setChanged();
    }

    private static ItemStack stackForTier(BigInteger tier) {
        if (tier.signum() <= 0) {
            return new ItemStack(Items.COBBLESTONE);
        }
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), tier);
        return stack;
    }

    public void scanRegion() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int r = band().decompressorRadius();
        int power = 0;
        int itemIn = 0;
        int itemOut = 0;
        int interfaces = 0;
        boolean ok = true;
        outer:
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    if (!level.isLoaded(p)) {
                        ok = false;
                        break outer;
                    }
                    BlockState s = level.getBlockState(p);
                    int boundary = (Math.abs(dx) == r ? 1 : 0)
                            + (Math.abs(dy) == r ? 1 : 0)
                            + (Math.abs(dz) == r ? 1 : 0);
                    if (boundary >= 2) {
                        if (s.is(ModBlocks.FRAME.get())) {
                        } else if (s.is(ModBlocks.PORT.get())) {
                            switch (s.getValue(CompressionPortBlock.PORT_TYPE)) {
                                case POWER -> power++;
                                case ITEM_IN -> itemIn++;
                                case ITEM_OUT -> itemOut++;
                                case SEED -> {
                                }
                            }
                        } else {
                            ok = false;
                            break outer;
                        }
                    } else if (boundary == 1) {
                        if (s.is(ModBlocks.GLASS.get())) {
                        } else if (s.is(ModBlocks.PORT.get())) {
                            switch (s.getValue(CompressionPortBlock.PORT_TYPE)) {
                                case POWER -> power++;
                                case ITEM_IN -> itemIn++;
                                case ITEM_OUT -> itemOut++;
                                case SEED -> {
                                }
                            }
                        } else if (s.is(ModBlocks.INTERFACE.get())) {
                            interfaces++;
                        } else {
                            ok = false;
                            break outer;
                        }
                    } else if (!s.isAir()) {
                        ok = false;
                        break outer;
                    }
                }
            }
        }
        boolean result = ok
                && power >= band().minPowerPorts()
                && itemIn >= CompressionConfig.MIN_ITEM_IN_PORTS
                && itemOut >= CompressionConfig.MIN_ITEM_OUT_PORTS
                && interfaces >= CompressionConfig.MIN_INTERFACES
                && interfaces <= CompressionConfig.MAX_INTERFACES;
        applyFormed(result);
    }

    private void applyFormed(boolean value) {
        if (this.formed == value) {
            return;
        }
        this.formed = value;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return bandedTitle("container.rcc.decompression_core");
    }

    // Header shows which band this multiblock is, so different-tier machines are distinguishable at a glance.
    private Component bandedTitle(String key) {
        MutableComponent title = Component.translatable(key);
        String numeral = DisplayNames.bandNumeral(band());
        if (!numeral.isEmpty()) {
            title.append(" " + numeral);
        }
        return title;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CompressionCoreMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TAG_SEED, seed.serializeNBT(registries));
        tag.put(TAG_OUTPUT, output.serializeNBT(registries));
        tag.putInt(TAG_ENERGY, energy);
        tag.putDouble(TAG_BCE_POOL, bcePool);
        tag.putString(TAG_GOAL, goalTier.toString());
        tag.putInt(TAG_PROGRESS, progress);
        tag.putBoolean(TAG_FORMED, formed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_SEED)) {
            seed.deserializeNBT(registries, tag.getCompound(TAG_SEED));
        }
        if (tag.contains(TAG_OUTPUT)) {
            output.deserializeNBT(registries, tag.getCompound(TAG_OUTPUT));
        }
        this.energy = tag.getInt(TAG_ENERGY);
        this.bcePool = tag.contains(TAG_BCE_POOL) ? tag.getDouble(TAG_BCE_POOL) : Double.NEGATIVE_INFINITY;
        this.goalTier = parseGoal(tag.getString(TAG_GOAL));
        this.progress = tag.getInt(TAG_PROGRESS);
        this.formed = tag.getBoolean(TAG_FORMED);
    }

    private static BigInteger parseGoal(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigInteger.ZERO;
        }
        try {
            BigInteger value = new BigInteger(raw.trim());
            return value.signum() < 0 ? BigInteger.ZERO : value;
        } catch (NumberFormatException ignored) {
            return BigInteger.ZERO;
        }
    }
}
