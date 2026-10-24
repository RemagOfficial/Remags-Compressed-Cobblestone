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

/**
 * Controller of the single-block {@link CompressionAutoDecompressorBlock}: the reverse of the Auto-Compressor. It
 * takes a compressed-cobblestone seed plus FE and steps it <em>down</em> one tier at a time until it reaches the
 * player-selected goal tier, depositing the released mass into a {@link IBceStorage BCE} buffer and ejecting the
 * downgraded block to the output slot.
 *
 * <p>Mass is conserved exactly against the compressor: a step {@code t -> t-1} releases {@code 8 * 9^(t-1)} BCE
 * ({@link CompressionConfig#requiredStepMassLog}), scaled by the seed stack size. Decompressing a tier-{@code t0}
 * block to tier {@code g} therefore releases {@code 9^t0 - 9^g} BCE and leaves a tier-{@code g} block worth
 * {@code 9^g}. The buffer's ceiling ({@link CompressionConfig#DECOMP_BCE_CAPACITY_LOG}) is set effectively
 * unlimited while nothing can drain it - the GUI measures the pool against
 * {@link CompressionConfig#DECOMP_BCE_DISPLAY_LOG} instead - so a descent never stalls for lack of room.
 *
 * <p>It drives the shared {@link CompressionCoreMenu}/{@link com.remag.rcc.client.CompressionCoreScreen} (seed slot,
 * output slot, fuel/progress/energy gauges) by implementing {@link ICompressionMachine}; the goal-tier text field is
 * surfaced because it also implements {@link IDecompressTarget}. All logic is server-authoritative.
 */
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
    // Seed-in / output-out view exposed on every face; the BCE product leaves through the separate bceView cap.
    private final DecompressorItemHandler itemHandler = new DecompressorItemHandler(this);

    // FE buffer kept as a plain count; the exposed view is receive-only (see the compressor for why we wrap a view).
    private int energy = 0;
    private final IEnergyStorage energyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return CompressionAutoDecompressorBlockEntity.this.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0; // automation cannot pull energy back out
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

    /** Accumulated BCE output in log10 space; {@link Double#NEGATIVE_INFINITY} means empty. */
    private double bcePool = Double.NEGATIVE_INFINITY;
    /** The tier the seed is decompressed down to. */
    private BigInteger goalTier = BigInteger.ZERO;
    /** 0 when idle, otherwise counting up to {@link CompressionConfig#AUTO_TICKS_PER_STEP}. */
    private int progress = 0;

    /** The BCE buffer as a sided capability view (extractable by future pipes). */
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
                return Double.NEGATIVE_INFINITY; // would overflow the buffer
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
            double take = Math.min(amountLog, pool); // withdraw up to what is present
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

    // --- ICompressionMachine / IDecompressTarget / capability accessors ------------------------

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

    /** The fuel gauge is repurposed to read as BCE buffer fill. */
    @Override
    public double getFuelPool() {
        return bcePool;
    }

    /**
     * The gauge's denominator: the tier-1000 display quantum ({@link CompressionConfig#DECOMP_BCE_DISPLAY_LOG}).
     * The real capacity is effectively unlimited and would leave the bar pinned at 0%.
     */
    @Override
    public double getRequiredFuel() {
        return CompressionConfig.DECOMP_BCE_DISPLAY_LOG;
    }

    /** A single block is always ready; there is no surrounding structure to validate. */
    @Override
    public boolean isFormed() {
        return true;
    }

    /** The decompressor does not burn fuel; a fed stack is converted straight into BCE mass. */
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
            massLog = 0.0; // vanilla cobblestone = 1 BCE (9^0) -> log10 = 0
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
            return null; // only a compressed block is a valid seed (vanilla has nothing to decompress)
        }
        return stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
    }

    /** log10 BCE the next step would release for the whole seed stack: per-unit differential scaled by stack size. */
    private double releaseMassLog() {
        BigInteger t = seedTier();
        if (t == null || t.signum() <= 0) {
            return Double.NEGATIVE_INFINITY;
        }
        int count = seed.getStackInSlot(0).getCount();
        double perUnit = CompressionConfig.requiredStepMassLog(t.subtract(BigInteger.ONE));
        return count > 1 ? perUnit + Math.log10(count) : perUnit;
    }

    // --- Cycle logic ---------------------------------------------------------------------------

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
            return false; // no seed, or already at/below the goal tier
        }
        if (energy < CompressionConfig.AUTO_ENERGY_PER_STEP) {
            return false;
        }
        // Safety net, not a gameplay gate: the capacity is effectively unlimited, so this only rejects a corrupt
        // pool (NaN/Infinity from a bad save or a misbehaving pipe) rather than ever pausing a legit descent.
        double release = releaseMassLog();
        if (!(CompressionConfig.log10add(bcePool, release) <= CompressionConfig.DECOMP_BCE_CAPACITY_LOG)) {
            return false;
        }
        // Only the final step (reaching the goal) ejects an item; require output room for the whole seed stack then.
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
        ItemStack result = stackForTier(next); // tier 0 becomes a vanilla cobblestone
        result.setCount(seedCount);
        this.seed.extractItem(0, seedCount, false);
        if (next.compareTo(goalTier) > 0) {
            this.seed.insertItem(0, result, false); // still above the goal: keep decompressing in place
        } else {
            this.output.insertItem(0, result, false); // reached the goal: eject the downgraded block
        }
        setChanged();
    }

    /** A one-unit stack for the given tier: vanilla cobblestone at tier 0, otherwise a tiered compressed block. */
    private static ItemStack stackForTier(BigInteger tier) {
        if (tier.signum() <= 0) {
            return new ItemStack(Items.COBBLESTONE);
        }
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), tier);
        return stack;
    }

    // --- MenuProvider (opened directly on the block) -------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rcc.compression_auto_decompressor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CompressionCoreMenu(containerId, playerInventory, this);
    }

    // --- Persistence ---------------------------------------------------------------------------

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
