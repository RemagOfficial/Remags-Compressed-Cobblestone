package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.IBceStorage;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.menu.CompressionCoreMenu;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public class CompressionAutoDecompressorBlockEntity extends BlockEntity
        implements MenuProvider, ICompressionMachine, IDecompressTarget {
    private static final String TAG_SEED = "seed";
    private static final String TAG_OUTPUT = "output";
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_BCE_POOL = "bcePool";
    private static final String TAG_GOAL = "goalTier";
    private static final String TAG_PROGRESS = "progress";

    private final ItemStackHandler seed = new ItemStackHandler(1);
    private final ItemStackHandler output = new ItemStackHandler(1);
    private final DecompressorItemHandler itemHandler = new DecompressorItemHandler(this);

    private int energy = 0;
    private final IEnergyStorage energyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return CompressionAutoDecompressorBlockEntity.this.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return CompressionAutoDecompressorBlockEntity.this.energy;
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

    private final IBceStorage bceView = new IBceStorage() {
        @Override
        public double getBceStoredLog() {
            return CompressionAutoDecompressorBlockEntity.this.bcePool;
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
            double result = CompressionConfig.log10add(
                    CompressionAutoDecompressorBlockEntity.this.bcePool, amountLog);
            if (result > CompressionConfig.DECOMP_BCE_CAPACITY_LOG) {
                return Double.NEGATIVE_INFINITY;
            }
            if (!simulate) {
                CompressionAutoDecompressorBlockEntity.this.bcePool = result;
                setChanged();
            }
            return amountLog;
        }

        @Override
        public double extractBce(double amountLog, boolean simulate) {
            if (!canExtract()) {
                return Double.NEGATIVE_INFINITY;
            }
            double pool = CompressionAutoDecompressorBlockEntity.this.bcePool;
            if (pool == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            double take = Math.min(amountLog, pool);
            if (!simulate) {
                CompressionAutoDecompressorBlockEntity.this.bcePool = CompressionConfig.log10subtract(pool, take);
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

    public CompressionAutoDecompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSION_AUTO_DECOMPRESSOR.get(), pos, state);
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

    public DecompressorItemHandler getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    public IBceStorage getBceStorage(@Nullable Direction side) {
        return bceView;
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public int getProgressMax() {
        return CompressionConfig.AUTO_TICKS_PER_STEP;
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
        return true;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, CompressionAutoDecompressorBlockEntity be) {
        if (be.progress > 0) {
            int perTick = Math.max(1, CompressionConfig.AUTO_ENERGY_PER_STEP / CompressionConfig.AUTO_TICKS_PER_STEP);
            be.drainEnergy(Math.min(perTick, be.energy));
            if (++be.progress >= CompressionConfig.AUTO_TICKS_PER_STEP) {
                be.completeStep();
            } else {
                be.setChanged();
            }
        } else if (be.canStep()) {
            be.progress = 1;
            be.setChanged();
        }
    }

    private boolean canStep() {
        if (progress > 0) {
            return false;
        }
        BigInteger t = seedTier();
        if (t == null || t.compareTo(goalTier) <= 0) {
            return false;
        }
        if (energy < CompressionConfig.AUTO_ENERGY_PER_STEP) {
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rcc.compression_auto_decompressor");
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
