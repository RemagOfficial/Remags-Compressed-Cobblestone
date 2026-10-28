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

public class CompressionAutoBlockEntity extends BlockEntity implements MenuProvider, ICompressionMachine, ICompressTarget {
    private static final String TAG_SEED = "seed";
    private static final String TAG_OUTPUT = "output";
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_FUEL_POOL = "fuelPool";
    private static final String TAG_GOAL = "goalTier";
    private static final String TAG_PROGRESS = "progress";

    private final ItemStackHandler seed = new ItemStackHandler(1);
    private final ItemStackHandler output = new ItemStackHandler(1);
    private final AutoItemHandler itemHandler = new AutoItemHandler(this);

    private int energy = 0;
    private final IEnergyStorage energyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return CompressionAutoBlockEntity.this.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return CompressionAutoBlockEntity.this.energy;
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

    private double fuelPool = Double.NEGATIVE_INFINITY;
    private int progress = 0;
    private BigInteger goalTier = CompressionConfig.AUTO_TARGET_LEVEL;

    private final IBceStorage bceFuelView = new IBceStorage() {
        @Override
        public double getBceStoredLog() {
            return CompressionAutoBlockEntity.this.fuelPool;
        }

        @Override
        public double getMaxBceLog() {
            return CompressionConfig.AUTO_BCE_CAPACITY_LOG;
        }

        @Override
        public double receiveBce(double amountLog, boolean simulate) {
            if (!canReceive() || amountLog == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            double free = CompressionConfig.log10subtract(
                    CompressionConfig.AUTO_BCE_CAPACITY_LOG, CompressionAutoBlockEntity.this.fuelPool);
            if (free == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            double accepted = Math.min(amountLog, free);
            if (!simulate) {
                double merged = CompressionConfig.log10add(
                        CompressionAutoBlockEntity.this.fuelPool, accepted);
                CompressionAutoBlockEntity.this.fuelPool =
                        Math.min(merged, CompressionConfig.AUTO_BCE_CAPACITY_LOG);
                setChanged();
            }
            return accepted;
        }

        @Override
        public double extractBce(double amountLog, boolean simulate) {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        @Override
        public boolean canExtract() {
            return false;
        }
    };

    public CompressionAutoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSION_AUTO.get(), pos, state);
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

    public AutoItemHandler getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    public IBceStorage getBceFuel(@Nullable Direction side) {
        return this.bceFuelView;
    }

    @Override
    public boolean acceptsSeed(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ModItems.COMPRESSED_COBBLESTONE.get()) || stack.is(Items.COBBLESTONE));
    }

    @Override
    public BigInteger getGoalTier() {
        return goalTier;
    }

    @Override
    public void setGoalTier(BigInteger goal) {
        this.goalTier = (goal == null || goal.signum() <= 0)
                ? CompressionConfig.AUTO_TARGET_LEVEL
                : goal.min(CompressionConfig.AUTO_TARGET_LEVEL);
        setChanged();
    }

    @Override
    public String goalTierString() {
        return goalTier.toString();
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
        return fuelPool;
    }

    @Override
    public double getRequiredFuel() {
        return requiredStepFuel();
    }

    private double requiredStepFuel() {
        BigInteger n = seedTier();
        if (n == null) {
            return Double.NEGATIVE_INFINITY;
        }
        int count = seed.getStackInSlot(0).getCount();
        double perUnit = CompressionConfig.requiredStepMassLog(n);
        return count > 1 ? perUnit + Math.log10(count) : perUnit;
    }

    @Override
    public boolean isFormed() {
        return true;
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

    @Override
    public void acceptFuel(ItemStack stack) {
        double massLog;
        if (stack.is(ModItems.COMPRESSED_COBBLESTONE.get())) {
            BigInteger tier = stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
            massLog = CompressionConfig.bceMassLog(tier);
        } else {
            massLog = 0.0;
        }
        massLog += Math.log10(stack.getCount());
        this.fuelPool = CompressionConfig.log10add(this.fuelPool, massLog);
        setChanged();
    }

    @Nullable
    private BigInteger seedTier() {
        ItemStack stack = seed.getStackInSlot(0);
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(ModItems.COMPRESSED_COBBLESTONE.get())) {
            return stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
        }
        if (stack.is(Items.COBBLESTONE)) {
            return BigInteger.ZERO;
        }
        return null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CompressionAutoBlockEntity be) {
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
        BigInteger n = seedTier();
        if (n == null || n.compareTo(goalTier) >= 0) {
            return false;
        }
        if (!(fuelPool >= requiredStepFuel())) {
            return false;
        }
        if (energy < CompressionConfig.AUTO_ENERGY_PER_STEP) {
            return false;
        }
        if (n.add(BigInteger.ONE).compareTo(goalTier) >= 0) {
            ItemStack probe = tierStack(goalTier);
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
        if (t == null || t.compareTo(goalTier) >= 0) {
            setChanged();
            return;
        }
        int seedCount = this.seed.getStackInSlot(0).getCount();
        double spend = CompressionConfig.requiredStepMassLog(t)
                + (seedCount > 1 ? Math.log10(seedCount) : 0.0);
        this.fuelPool = CompressionConfig.log10subtract(this.fuelPool, spend);
        BigInteger next = t.add(BigInteger.ONE);
        ItemStack upgraded = tierStack(next);
        upgraded.setCount(seedCount);
        this.seed.extractItem(0, seedCount, false);
        if (next.compareTo(goalTier) < 0) {
            this.seed.insertItem(0, upgraded, false);
        } else {
            this.output.insertItem(0, upgraded, false);
        }
        setChanged();
    }

    private static ItemStack tierStack(BigInteger tier) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), tier);
        return stack;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rcc.compression_auto");
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
        tag.putDouble(TAG_FUEL_POOL, fuelPool);
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
        this.fuelPool = tag.contains(TAG_FUEL_POOL) ? tag.getDouble(TAG_FUEL_POOL) : Double.NEGATIVE_INFINITY;
        this.goalTier = parseGoal(tag.getString(TAG_GOAL));
        this.progress = tag.getInt(TAG_PROGRESS);
    }

    private static BigInteger parseGoal(String raw) {
        if (raw == null || raw.isBlank()) {
            return CompressionConfig.AUTO_TARGET_LEVEL;
        }
        try {
            BigInteger value = new BigInteger(raw.trim());
            return value.signum() <= 0 ? CompressionConfig.AUTO_TARGET_LEVEL : value.min(CompressionConfig.AUTO_TARGET_LEVEL);
        } catch (NumberFormatException ignored) {
            return CompressionConfig.AUTO_TARGET_LEVEL;
        }
    }
}
