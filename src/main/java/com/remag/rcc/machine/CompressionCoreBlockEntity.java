package com.remag.rcc.machine;

import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.menu.CompressionCoreMenu;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import java.util.Objects;

/**
 * Controller of the Compression Manipulator. Holds the seed slot, a base-cobblestone-equivalent fuel-mass
 * pool (kept in log10 space so it stays finite toward a googolplex tier), an FE buffer, the finished-output
 * slot, and the doubling cycle state. The {@link CompressionPortBlockEntity}s forward their capabilities here;
 * this block entity runs the only server ticker.
 *
 * <p>A cycle turns a seed tier {@code n} into {@code 2n} when the structure is {@code formed}, the seed is at
 * least {@link CompressionConfig#MANIPULATOR_MIN_IN}, doubling stays within
 * {@link CompressionConfig#MANIPULATOR_MAX_OUT}, enough fuel mass has been fed, and enough FE is stored. On
 * start the seed and the whole fuel pool are consumed and FE drains across the craft.
 */
public class CompressionCoreBlockEntity extends BlockEntity implements MenuProvider {
    private static final String TAG_SEED = "seed";
    private static final String TAG_OUTPUT = "output";
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_FUEL_POOL = "fuelPool";
    private static final String TAG_PROGRESS = "progress";
    private static final String TAG_FORMED = "formed";
    private static final String TAG_PENDING = "pendingResult";
    /** Update-tag-only key carrying the seed tier the client BER should render as the spinning cube. */
    private static final String TAG_RENDER_SEED = "renderSeed";

    private static final int RESCAN_INTERVAL = 20;

    private final ItemStackHandler seed = new ItemStackHandler(1);
    private final ItemStackHandler output = new ItemStackHandler(1);
    // Internal FE buffer. Kept as a plain count so the machine can spend it directly: a NeoForge
    // EnergyStorage with maxExtract 0 would also block its own extractEnergy, so we wrap a receive-only
    // view for the power port instead.
    private int energy = 0;
    private final IEnergyStorage energyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return CompressionCoreBlockEntity.this.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0; // external actors cannot pull energy back out
        }

        @Override
        public int getEnergyStored() {
            return CompressionCoreBlockEntity.this.energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return CompressionConfig.ENERGY_CAPACITY;
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
    /** 0 when idle, otherwise counting up to {@link CompressionConfig#TICKS_PER_CRAFT}. */
    private int progress = 0;
    /** Result tier of an in-progress craft, so a reload can finish what it started. */
    @Nullable
    private BigInteger pendingResult = null;
    /** Whether a valid structure currently surrounds this core; drives machine logic and the GUI, not the model. */
    private boolean formed = false;

    /**
     * Tier the client should render as the seed cube. Kept equal to the seed across a whole cycle (the slot is
     * emptied at craft start, but the cube keeps spinning until completion), then cleared. Server-authoritative.
     */
    @Nullable
    private BigInteger renderSeedTier = null;
    /** Last {@link #renderSeedTier} pushed to clients, so {@code serverTick} only re-syncs when it actually changes. */
    @Nullable
    private BigInteger syncedRenderSeedTier = null;
    /** Client-side mirror of {@link #renderSeedTier}, read by {@link com.remag.rcc.client.CompressionCoreRenderer}. */
    @Nullable
    private BigInteger clientSeedTier = null;

    private int rescanCooldown = 0;

    public CompressionCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSION_CORE.get(), pos, state);
    }

    // --- Capability / GUI accessors ------------------------------------------------------------

    public IEnergyStorage getEnergy() {
        return energyView;
    }

    private int receiveEnergy(int maxReceive, boolean simulate) {
        int toStore = Math.min(CompressionConfig.ENERGY_CAPACITY - this.energy, Math.max(0, maxReceive));
        if (!simulate) {
            this.energy += toStore;
            setChanged();
        }
        return toStore;
    }

    private void drainEnergy(int amount) {
        this.energy = Math.max(0, this.energy - Math.max(0, amount));
    }

    public ItemStackHandler getOutput() {
        return output;
    }

    public ItemStackHandler getSeed() {
        return seed;
    }

    public boolean isFormed() {
        return formed;
    }

    public double getFuelPool() {
        return fuelPool;
    }

    public int getProgress() {
        return progress;
    }

    /** Seed tier the client BER should render as the spinning cube, or {@code null} when there is nothing to show. */
    @Nullable
    public BigInteger getRenderSeedTier() {
        return clientSeedTier;
    }

    /** True while a cycle is running; the client uses this to decide whether to spin the cube. */
    public boolean isCrafting() {
        return progress > 0;
    }

    /** Current fuel requirement (log10 mass) for the seed in the slot, or 0 when there is no valid seed. */
    public double getRequiredFuel() {
        BigInteger n = seedTier();
        return n == null ? 0.0 : CompressionConfig.requiredMassLog(n);
    }

    @Nullable
    private BigInteger seedTier() {
        ItemStack stack = seed.getStackInSlot(0);
        if (stack.isEmpty() || !stack.is(ModItems.COMPRESSED_COBBLESTONE.get())) {
            return null;
        }
        return stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
    }

    // --- Fuel intake (driven by ITEM_IN ports) -------------------------------------------------

    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.COMPRESSED_COBBLESTONE.get()) || stack.is(Items.COBBLESTONE));
    }

    /** Only a compressed-cobblestone item may sit in the seed slot (vanilla cobblestone is fuel-only). */
    public static boolean isSeed(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.COMPRESSED_COBBLESTONE.get());
    }

    /**
     * Add a fed stack's mass to the pool. A tier-{@code t} block is worth {@code 9^(t-1)} base cobblestone and
     * vanilla cobblestone is worth {@code 1}; a stack of {@code k} of them multiplies that by {@code k}. All
     * arithmetic is in log10 space so even astronomically large tiers stay representable.
     */
    public void acceptFuel(ItemStack stack) {
        double massLog;
        if (stack.is(ModItems.COMPRESSED_COBBLESTONE.get())) {
            BigInteger tier = stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
            massLog = CompressionConfig.tierMassLog(tier);
        } else {
            massLog = 0.0; // vanilla cobblestone: 9^0 = 1 -> log10 = 0
        }
        massLog += Math.log10(stack.getCount());
        this.fuelPool = CompressionConfig.log10add(this.fuelPool, massLog);
        setChanged();
    }

    // --- Cycle logic ---------------------------------------------------------------------------

    private boolean canStart() {
        if (!formed || progress > 0 || !output.getStackInSlot(0).isEmpty()) {
            return false;
        }
        BigInteger n = seedTier();
        if (n == null || n.compareTo(CompressionConfig.MANIPULATOR_MIN_IN) < 0) {
            return false;
        }
        if (n.shiftLeft(1).compareTo(CompressionConfig.MANIPULATOR_MAX_OUT) > 0) {
            return false;
        }
        if (!(fuelPool >= CompressionConfig.requiredMassLog(n))) {
            return false;
        }
        return energy >= CompressionConfig.ENERGY_PER_CRAFT;
    }

    private void startCraft() {
        BigInteger n = seedTier();
        if (n == null) {
            return;
        }
        this.pendingResult = n.shiftLeft(1);
        // Freeze the render tier NOW, from the seed we are about to consume. This is what guarantees the client
        // BER has a tier to draw even when the machine never idles (a freshly-loaded, instantly-ready manipulator
        // starts crafting on its first tick, so syncRenderSeedTier's idle branch would never run otherwise).
        this.renderSeedTier = n;
        // Consume exactly ONE seed per craft: a stack in the seed slot is a queue, so the rest survive for later
        // cycles instead of being wiped when only tier n is actually used.
        this.seed.extractItem(0, 1, false);
        this.fuelPool = Double.NEGATIVE_INFINITY; // the whole pool is spent (overfeed is lost)
        this.progress = 1;
        setChanged();
        sync();
    }

    private void completeCraft() {
        BigInteger result = this.pendingResult;
        if (result != null && output.getStackInSlot(0).isEmpty()) {
            output.setStackInSlot(0, tierStack(result));
        }
        this.pendingResult = null;
        this.progress = 0;
        setChanged();
        sync();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CompressionCoreBlockEntity be) {
        if (--be.rescanCooldown <= 0) {
            be.rescanCooldown = RESCAN_INTERVAL;
            be.scanRegion();
        }
        if (be.progress > 0) {
            int perTick = Math.max(1, CompressionConfig.ENERGY_PER_CRAFT / CompressionConfig.TICKS_PER_CRAFT);
            be.drainEnergy(Math.min(perTick, be.energy));
            if (++be.progress >= CompressionConfig.TICKS_PER_CRAFT) {
                be.completeCraft();
            } else {
                be.setChanged();
            }
        } else if (be.canStart()) {
            be.startCraft();
        }
        be.syncRenderSeedTier();
    }

    /**
     * Keep {@link #renderSeedTier} in step with the seed that owns the current view: the live seed while idle, or
     * the cycle's seed (captured at {@link #startCraft} and frozen while crafting) otherwise. Push it to clients
     * only when it changes so the {@link com.remag.rcc.client.CompressionCoreRenderer} cube appears/updates/disappears.
     */
    private void syncRenderSeedTier() {
        if (this.progress == 0) {
            this.renderSeedTier = seedTier();
        }
        if (!Objects.equals(this.renderSeedTier, this.syncedRenderSeedTier)) {
            this.syncedRenderSeedTier = this.renderSeedTier;
            sync();
        }
    }

    private static ItemStack tierStack(BigInteger tier) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), tier);
        return stack;
    }

    // --- Structure validation ------------------------------------------------------------------

    /**
     * Verify the exact 5x5x5 blueprint around this core and update {@code formed}. Cells are classified by how
     * many of their coordinates sit on the cube boundary (equal to &plusmn;{@code radius}):
     * <ul>
     *   <li>two or three boundary coordinates (edges/corners) must be {@link ModBlocks#FRAME frame};</li>
     *   <li>exactly one boundary coordinate (face panels) must be {@link ModBlocks#GLASS glass}, a
     *       {@link ModBlocks#PORT port}, or the {@link ModBlocks#INTERFACE interface};</li>
     *   <li>no boundary coordinate (the interior 3x3x3) must be air, except this core at the centre.</li>
     * </ul>
     * At least one port of each kind is required, and the interface count must be exactly
     * {@link CompressionConfig#MIN_INTERFACES} to {@link CompressionConfig#MAX_INTERFACES} (the sole GUI entry).
     */
    public void scanRegion() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int r = CompressionConfig.REGION_RADIUS;
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
                        if (!s.is(ModBlocks.FRAME.get())) {
                            ok = false;
                            break outer;
                        }
                    } else if (boundary == 1) {
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
                        ok = false;
                        break outer;
                    }
                }
            }
        }
        boolean result = ok
                && power >= CompressionConfig.MIN_POWER_PORTS
                && itemIn >= CompressionConfig.MIN_ITEM_IN_PORTS
                && itemOut >= CompressionConfig.MIN_ITEM_OUT_PORTS
                && interfaces >= CompressionConfig.MIN_INTERFACES
                && interfaces <= CompressionConfig.MAX_INTERFACES;
        applyFormed(result);
    }

    /**
     * Locate the core this shell cell belongs to, scanning the {@code regionRadius} cube around {@code from}.
     * Shared by {@link CompressionInterfaceBlock} (which has no block entity of its own) to reach the controller.
     */
    @Nullable
    public static CompressionCoreBlockEntity findCore(Level level, BlockPos from) {
        int r = CompressionConfig.REGION_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = from.offset(dx, dy, dz);
                    if (level.getBlockState(p).is(ModBlocks.CORE.get())
                            && level.getBlockEntity(p) instanceof CompressionCoreBlockEntity core) {
                        return core;
                    }
                }
            }
        }
        return null;
    }

    // --- MenuProvider (opened via the interface block) -----------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rcc.compression_core");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CompressionCoreMenu(containerId, playerInventory, this);
    }

    private void applyFormed(boolean value) {
        if (this.formed == value) {
            return;
        }
        this.formed = value;
        setChanged();
        // Push the flag to clients (for the GUI and any future "is working" visual); the blockstate is unchanged.
        sync();
    }

    // --- Persistence & client sync -------------------------------------------------------------

    private void sync() {
        if (level != null && !level.isClientSide()) {
            // Trigger getUpdatePacket() delivery to tracking clients. The client applies it via onDataPacket, which we
            // override to route into handleUpdateTag so the render tier updates live (not just on chunk load).
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TAG_SEED, seed.serializeNBT(registries));
        tag.put(TAG_OUTPUT, output.serializeNBT(registries));
        tag.putInt(TAG_ENERGY, energy);
        tag.putDouble(TAG_FUEL_POOL, fuelPool);
        tag.putInt(TAG_PROGRESS, progress);
        tag.putBoolean(TAG_FORMED, formed);
        if (pendingResult != null) {
            tag.putString(TAG_PENDING, pendingResult.toString());
        }
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
        this.progress = tag.getInt(TAG_PROGRESS);
        this.formed = tag.getBoolean(TAG_FORMED);
        this.pendingResult = tag.contains(TAG_PENDING) ? new BigInteger(tag.getString(TAG_PENDING)) : null;
        // Re-derive the render tier after a (re)load. A running craft stores its result as pendingResult = seed << 1,
        // so the seed the cube should show is pendingResult >> 1. Without this, rejoining mid-craft leaves
        // renderSeedTier null (its idle branch only runs when progress == 0), so no cube appears until the next craft.
        if (this.pendingResult != null) {
            this.renderSeedTier = this.pendingResult.shiftRight(1);
        }
    }

    /** Only the render/GUI-relevant scalars go to clients; inventories and energy stay server-side. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_PROGRESS, progress);
        tag.putBoolean(TAG_FORMED, formed);
        tag.putDouble(TAG_FUEL_POOL, fuelPool);
        if (this.renderSeedTier != null) {
            tag.putString(TAG_RENDER_SEED, this.renderSeedTier.toString());
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.progress = tag.getInt(TAG_PROGRESS);
        this.formed = tag.getBoolean(TAG_FORMED);
        this.fuelPool = tag.getDouble(TAG_FUEL_POOL);
        this.clientSeedTier = tag.contains(TAG_RENDER_SEED) ? new BigInteger(tag.getString(TAG_RENDER_SEED)) : null;
    }

    /**
     * NeoForge's default {@code onDataPacket} applies a live block-entity update packet through
     * {@code loadWithComponents} -> {@code loadAdditional}, which never reads the client-only render tier. That is why
     * the cube appeared on chunk load (which uses {@code handleUpdateTag}) but not during a running craft. Route the
     * packet into {@link #handleUpdateTag} so {@link #clientSeedTier} updates live.
     */
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
