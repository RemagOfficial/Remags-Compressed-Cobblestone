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
 * Controller of the single-block {@link CompressionAutoBlock}. It automates the manual 3x3 ladder: hold a seed,
 * burn fed cobblestone mass to upgrade it one tier at a time, and once it reaches
 * {@link CompressionConfig#AUTO_TARGET_LEVEL} (tier 1000) eject it to the output slot for the multiblock to consume.
 *
 * <p>Like the multiblock core it keeps a receive-only FE buffer and a log10-base-cobblestone fuel pool, but its
 * economy differs: each {@code t -> t+1} step consumes only the <em>differential</em> mass
 * ({@link CompressionConfig#requiredStepMassLog}) from the pool rather than the whole pool, so a single large feed
 * powers many steps up the ladder. Steps are timed ({@link CompressionConfig#AUTO_TICKS_PER_STEP} ticks each) and
 * drain {@link CompressionConfig#AUTO_ENERGY_PER_STEP} FE across them.
 *
 * <p>Fuel arrives two ways and lands in the same pool, because BCE <em>is</em> log10 cobblestone-equivalent mass - the
 * machine's native unit: as items through the item handler ({@link #acceptFuel}), or as BCE through an insert-mode BCE
 * pipe, which sees the {@link #getBceFuel sink-only view} of the capability. A pipe can never take fuel back out, so a
 * line cannot be looped to launder its own contents.
 *
 * <p>It drives the shared {@link CompressionCoreMenu}/{@link com.remag.rcc.client.CompressionCoreScreen} by
 * implementing {@link ICompressionMachine}, so the GUI (seed slot, output slot, fuel/progress/energy gauges) is
 * identical to the multiblock's. It exposes side-based capabilities directly (no port blocks).
 */
public class CompressionAutoBlockEntity extends BlockEntity implements MenuProvider, ICompressionMachine, ICompressTarget {
    private static final String TAG_SEED = "seed";
    private static final String TAG_OUTPUT = "output";
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_FUEL_POOL = "fuelPool";
    private static final String TAG_GOAL = "goalTier";
    private static final String TAG_PROGRESS = "progress";

    private final ItemStackHandler seed = new ItemStackHandler(1);
    private final ItemStackHandler output = new ItemStackHandler(1);
    // Insert=fuel / extract=output view exposed on every face, so pipes/hoppers can both feed the pool and pull
    // the finished tier-1000 block from the same capability without needing dedicated port blocks.
    private final AutoItemHandler itemHandler = new AutoItemHandler(this);

    // Internal FE buffer kept as a plain count so the machine can spend it directly; the exposed view is receive-only
    // (a NeoForge EnergyStorage with maxExtract 0 would also block our own extractEnergy, so we wrap a view).
    private int energy = 0;
    private final IEnergyStorage energyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return CompressionAutoBlockEntity.this.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0; // automation cannot pull energy back out
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

    /** Accumulated fuel mass in log10-base-cobblestone space; {@link Double#NEGATIVE_INFINITY} means empty. */
    private double fuelPool = Double.NEGATIVE_INFINITY;
    /** 0 when idle, otherwise counting up to {@link CompressionConfig#AUTO_TICKS_PER_STEP}. */
    private int progress = 0;
    /**
     * The tier the seed is climbed up to; the machine stops (and ejects the result) once it reaches this. Defaults to
     * the top tier so a fresh machine still runs all the way to 1000, but a player can lower it to stop partway and
     * uncraft the partial block back down by hand (e.g. to recover vanilla cobblestone).
     */
    private BigInteger goalTier = CompressionConfig.AUTO_TARGET_LEVEL;

    /**
     * The BCE face of {@link #fuelPool}: what a pipe sees when an insert end is placed against this machine. Same
     * log10 numbers, so no conversion exists to get wrong - one BCE is one vanilla cobblestone, which is exactly the
     * unit the pool is already kept in.
     *
     * <p>It is a pure <em>sink</em>: {@link #canExtract()} is false, so the fuel a bus has delivered can never be
     * pulled back out by the same or another pipe (an extract end pointed here simply finds nothing to read and is
     * skipped as a source). Like the storage tank it accepts <em>partially</em> rather than refusing an oversized
     * slice outright, so a bus can fill it right up to {@link CompressionConfig#AUTO_BCE_CAPACITY_LOG}.
     */
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
                return Double.NEGATIVE_INFINITY; // nothing a bus can push will land here until it is spent
            }
            double accepted = Math.min(amountLog, free);
            if (!simulate) {
                double merged = CompressionConfig.log10add(
                        CompressionAutoBlockEntity.this.fuelPool, accepted);
                // Clamp so rounding cannot leave the pool a hair above the ceiling, which would make `free` forever
                // positive and let the machine accept fuel it has no room for.
                CompressionAutoBlockEntity.this.fuelPool =
                        Math.min(merged, CompressionConfig.AUTO_BCE_CAPACITY_LOG);
                setChanged();
            }
            return accepted;
        }

        @Override
        public double extractBce(double amountLog, boolean simulate) {
            return Double.NEGATIVE_INFINITY; // fuel is one-way: see canExtract()
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

    // --- ICompressionMachine / capability accessors --------------------------------------------

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

    /** The combined fuel-in / output-out handler exposed on all faces (see {@link AutoItemHandler}). */
    public AutoItemHandler getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    /**
     * The sink-only BCE view of the fuel pool, offered on every face so an insert-mode pipe end can burn BCE here the
     * same way a hopper feeds cobblestone in (see {@link #bceFuelView}).
     */
    public IBceStorage getBceFuel(@Nullable Direction side) {
        return this.bceFuelView;
    }

    /** Accepts compressed cobblestone of any tier AND plain vanilla cobblestone as the tier-1 starting seed. */
    @Override
    public boolean acceptsSeed(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ModItems.COMPRESSED_COBBLESTONE.get()) || stack.is(Items.COBBLESTONE));
    }

    // --- ICompressTarget (goal tier) ------------------------------------------------------------

    @Override
    public BigInteger getGoalTier() {
        return goalTier;
    }

    /**
     * Clamp a submitted goal into {@code [1, }{@link CompressionConfig#AUTO_TARGET_LEVEL}{@code ]}. A null / non-positive
     * value falls back to the default (compress all the way), so a blank field never strands the machine at tier 0.
     */
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

    /**
     * Fuel (log10 BCE) the next step needs for the whole seed stack: the per-unit tier differential scaled by the
     * number of blocks in the seed slot, so a stack of seeds is charged for every upgrade it contains.
     */
    private double requiredStepFuel() {
        BigInteger n = seedTier();
        if (n == null) {
            return Double.NEGATIVE_INFINITY;
        }
        int count = seed.getStackInSlot(0).getCount();
        double perUnit = CompressionConfig.requiredStepMassLog(n);
        return count > 1 ? perUnit + Math.log10(count) : perUnit;
    }

    /** A single block is always ready; there is no surrounding structure to validate. */
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
            massLog = 0.0; // vanilla cobblestone = 1 BCE (9^0) -> log10 = 0
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
            return BigInteger.ZERO; // a plain cobblestone seed starts the ladder at tier 0
        }
        return null;
    }

    // --- Cycle logic ---------------------------------------------------------------------------

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
            return false; // nothing to upgrade (no seed, or already at/above the goal)
        }
        if (!(fuelPool >= requiredStepFuel())) {
            return false;
        }
        if (energy < CompressionConfig.AUTO_ENERGY_PER_STEP) {
            return false;
        }
        // Only the final step produces an output item; intermediate steps upgrade the seed in place, so they never
        // need output room. Require space for the whole seed stack as goal-tier results.
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
        // Spend exactly this step's differential mass (scaled by the seed stack size); any surplus stays pooled.
        int seedCount = this.seed.getStackInSlot(0).getCount();
        double spend = CompressionConfig.requiredStepMassLog(t)
                + (seedCount > 1 ? Math.log10(seedCount) : 0.0);
        this.fuelPool = CompressionConfig.log10subtract(this.fuelPool, spend);
        BigInteger next = t.add(BigInteger.ONE);
        // Always rebuild the seed as a real compressed-cobblestone stack at the new tier (a vanilla-cobblestone seed
        // is converted here on its first step), preserving the stack size.
        ItemStack upgraded = tierStack(next);
        upgraded.setCount(seedCount);
        this.seed.extractItem(0, seedCount, false);
        if (next.compareTo(goalTier) < 0) {
            this.seed.insertItem(0, upgraded, false); // upgraded work-in-progress stays in the seed slot
        } else {
            this.output.insertItem(0, upgraded, false); // finished goal-tier result moves to the output slot
        }
        setChanged();
    }

    private static ItemStack tierStack(BigInteger tier) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), tier);
        return stack;
    }

    // --- MenuProvider (opened directly on the block) -------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rcc.compression_auto");
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

    /** Read a persisted goal, defaulting to the top tier when absent / blank / malformed / out of range. */
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
