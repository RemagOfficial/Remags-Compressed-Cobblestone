package com.remag.rcc.machine;

import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.IBceStorage;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.menu.CompressionCoreMenu;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

/**
 * Controller of the tier-1 <b>Decompression Manipulator</b>: the 3x3x3 multiblock counterpart of the single-block
 * {@link CompressionAutoDecompressorBlockEntity}. It takes a compressed-cobblestone seed plus FE, steps it
 * <em>down</em> one tier at a time toward a player-selected goal tier, banking the released mass into a
 * {@link IBceStorage BCE} buffer and ejecting the downgraded block once it reaches that goal - the exact reverse of
 * what the 5x5x5 {@link CompressionCoreBlockEntity Compression Manipulator} does.
 *
 * <p>Mass is conserved against the compressor: a step {@code t -> t-1} releases {@code 8 * 9^(t-1)} BCE
 * ({@link CompressionConfig#requiredStepMassLog}). The buffer ceiling is {@link CompressionConfig#DECOMP_BCE_CAPACITY_LOG},
 * effectively unlimited so a descent never stalls for room; the GUI measures the pool against
 * {@link CompressionConfig#DECOMP_BCE_DISPLAY_LOG}. Its own {@code DECOMP_MULTIBLOCK_*} constants time the descent at
 * one tier step per game tick - five times the single block's pace - for the same FE per step.
 *
 * <p>Unlike the single block it is <em>sealed</em> in the centre of a validated structure: it drives the shared
 * {@link CompressionCoreMenu}/{@link com.remag.rcc.client.CompressionCoreScreen} through {@link ICompressionMachine}
 * and surfaces the goal-tier field through {@link IDecompressTarget}, but is reached only through a
 * {@link CompressionInterfaceBlock} and linked to the outside world only through {@link CompressionPortBlock}s, whose
 * capabilities it answers via {@link IMultiblockController}. All logic is server-authoritative.
 */
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

    // FE buffer kept as a plain count; the exposed view is receive-only (mirrors the other machines - a NeoForge
    // EnergyStorage with maxExtract 0 would also block our own drain).
    private int energy = 0;
    private final IEnergyStorage energyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return DecompressorCoreBlockEntity.this.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0; // automation cannot pull energy back out
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

    /** Accumulated released BCE in log10 space; {@link Double#NEGATIVE_INFINITY} means empty. */
    private double bcePool = Double.NEGATIVE_INFINITY;
    /** The tier the seed is decompressed down to. */
    private BigInteger goalTier = BigInteger.ZERO;
    /** 0 when idle, otherwise counting up to {@link CompressionConfig#DECOMP_MULTIBLOCK_TICKS_PER_STEP}. */
    private int progress = 0;
    /** Whether a valid 3x3x3 structure currently surrounds this core; drives machine logic and the GUI. */
    private boolean formed = false;

    private int rescanCooldown = 0;

    /**
     * The BCE buffer as a sided capability view offered on the item ports: extractable so the released mass can be
     * piped out to BCE-powered machines, and receivable so mass can be fed back in - the same two-way pool the
     * single-block decompressor exposes. {@code canReceive()} accepting a write that would overflow is refused, just
     * as the standalone producer view does.
     */
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
                return Double.NEGATIVE_INFINITY; // would overflow the buffer
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
            double take = Math.min(amountLog, pool); // withdraw up to what is present
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

    /** The progression band this core's block belongs to; drives the shell radius, port floor and buffer ceiling. */
    public CompressionBand band() {
        return getBlockState().getBlock() instanceof DecompressorCoreBlock core
                ? core.band() : CompressionBand.BAND_1;
    }

    /** Sweep the capability cache across this band's shell box, not the band-1 default the interface carries. */
    @Override
    public int portShellExtent() {
        return band().shellExtent();
    }

    /**
     * The ports only carry the buffer once this core exists, and a pipe that asked one earlier cached the refusal
     * (see {@link IMultiblockPortHost#invalidatePortCaps}) - otherwise a tower finished by placing the core last
     * could neither be fed a seed nor drain its released mass until the chunk reloaded.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null) {
            IMultiblockPortHost.invalidatePortCaps(this.level, this.worldPosition, this.portShellExtent());
        }
    }

    // --- ICompressionMachine / IDecompressTarget accessors -------------------------------------

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

    /** The fuel gauge is repurposed to read as BCE buffer fill. */
    @Override
    public double getFuelPool() {
        return bcePool;
    }

    /** The gauge denominator: the tier-1000 display quantum (the real capacity is effectively unlimited). */
    @Override
    public double getRequiredFuel() {
        return CompressionConfig.DECOMP_BCE_DISPLAY_LOG;
    }

    @Override
    public boolean isFormed() {
        return formed;
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

    // --- IMultiblockController: what the shell ports forward -----------------------------------

    /**
     * ITEM_IN deposits a seed to break down ({@link SeedInsertHandler}), ITEM_OUT yields the finished downgraded
     * block, and POWER carries no items.
     */
    @Override
    @Nullable
    public IItemHandler portItemHandler(PortType type) {
        return switch (type) {
            case ITEM_IN -> new SeedInsertHandler(this);
            case ITEM_OUT -> output;
            case POWER -> null;
        };
    }

    /** The item ports expose the BCE buffer (drain the released mass out, or feed mass in); the POWER port does not. */
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
            // A one-tick step completes on the tick it starts, so the descent can release a tier <em>every</em> game
            // tick instead of every other one; longer step timings still just run the count-up branch above.
            if (be.progress >= CompressionConfig.DECOMP_MULTIBLOCK_TICKS_PER_STEP) {
                be.completeStep();
            }
            be.setChanged();
        }
    }

    /** Bill one tick's share of the step's FE. The start tick pays too, so a step always costs exactly its price. */
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
            return false; // no seed, or already at/below the goal tier
        }
        if (energy < CompressionConfig.DECOMP_MULTIBLOCK_ENERGY_PER_STEP) {
            return false;
        }
        // Safety net, not a gameplay gate: the capacity is effectively unlimited, so this only rejects a corrupt pool.
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

    // --- Structure validation (3x3x3, ports may replace the frame) -----------------------------

    /**
     * Verify the exact 3x3x3 blueprint around this core and update {@code formed}. Cells are classified by how many
     * of their coordinates sit on the cube boundary (equal to {@code radius = 1}):
     * <ul>
     *   <li>two or three boundary coordinates (the twelve edges and eight corners) - normally {@link ModBlocks#FRAME
     *       frame} - may <em>also</em> be a {@link ModBlocks#PORT port}, since the six face-centre cells alone cannot
     *       fit one interface plus one of every port kind;</li>
     *   <li>exactly one boundary coordinate (the six face centres) must be {@link ModBlocks#GLASS glass}, a port, or
     *       the {@link ModBlocks#INTERFACE interface} - the interface lives only here, so a port may replace the frame
     *       but never the interface's role;</li>
     *   <li>the centre (this core) is skipped.</li>
     * </ul>
     * At least one port of each kind is required, and the interface count must be exactly one. As with the 5x5x5
     * manipulator the whole check is a boolean, never a blockstate, so the core keeps a single model.
     */
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
                        continue; // this core
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
                        // Edges and corners: frame, or a port standing in for it (never glass, never the interface).
                        if (s.is(ModBlocks.FRAME.get())) {
                            // fine, a structural edge/corner
                        } else if (s.is(ModBlocks.PORT.get())) {
                            switch (s.getValue(CompressionPortBlock.PORT_TYPE)) {
                                case POWER -> power++;
                                case ITEM_IN -> itemIn++;
                                case ITEM_OUT -> itemOut++;
                            }
                        } else {
                            ok = false;
                            break outer;
                        }
                    } else if (boundary == 1) {
                        // Face centres: glass, a port, or the interface.
                        if (s.is(ModBlocks.GLASS.get())) {
                            // fine, a face panel
                        } else if (s.is(ModBlocks.PORT.get())) {
                            switch (s.getValue(CompressionPortBlock.PORT_TYPE)) {
                                case POWER -> power++;
                                case ITEM_IN -> itemIn++;
                                case ITEM_OUT -> itemOut++;
                            }
                        } else if (s.is(ModBlocks.INTERFACE.get())) {
                            interfaces++;
                        } else {
                            ok = false;
                            break outer;
                        }
                    } else if (!s.isAir()) {
                        // Interior (no boundary coordinate): hollow air, as in the compressor - band 1 has none.
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

    // --- MenuProvider (opened through the interface block) -------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rcc.decompression_core");
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
