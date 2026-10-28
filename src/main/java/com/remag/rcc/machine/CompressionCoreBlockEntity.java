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
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Objects;

public class CompressionCoreBlockEntity extends BlockEntity implements MenuProvider, ICompressionMachine, IMultiblockController {
    private static final String TAG_SEED = "seed";
    private static final String TAG_OUTPUT = "output";
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_FUEL_POOL = "fuelPool";
    private static final String TAG_PROGRESS = "progress";
    private static final String TAG_FORMED = "formed";
    private static final String TAG_PENDING = "pendingResult";
    private static final String TAG_RENDER_SEED = "renderSeed";
    private static final String TAG_CRAFTS_DONE = "craftsDone";

    private static final int RESCAN_INTERVAL = 20;

    private final ItemStackHandler seed = new ItemStackHandler(1);
    private final ItemStackHandler output = new ItemStackHandler(1);
    private int energy = 0;
    private final IEnergyStorage energyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return CompressionCoreBlockEntity.this.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
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

    private double fuelPool = Double.NEGATIVE_INFINITY;
    private final IBceStorage bceFuelView = new IBceStorage() {
        @Override
        public double getBceStoredLog() {
            return CompressionCoreBlockEntity.this.fuelPool;
        }

        @Override
        public double getMaxBceLog() {
            return CompressionCoreBlockEntity.this.band().sinkCeilingLog();
        }

        @Override
        public double receiveBce(double amountLog, boolean simulate) {
            if (!canReceive() || amountLog == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            double free = CompressionConfig.log10subtract(
                    CompressionCoreBlockEntity.this.band().sinkCeilingLog(), CompressionCoreBlockEntity.this.fuelPool);
            if (free == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            double accepted = Math.min(amountLog, free);
            if (!simulate) {
                double merged = CompressionConfig.log10add(
                        CompressionCoreBlockEntity.this.fuelPool, accepted);
                CompressionCoreBlockEntity.this.fuelPool =
                        Math.min(merged, CompressionCoreBlockEntity.this.band().sinkCeilingLog());
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
    private int progress = 0;
    @Nullable
    private BigInteger pendingResult = null;
    private boolean formed = false;

    @Nullable
    private BigInteger renderSeedTier = null;
    @Nullable
    private BigInteger syncedRenderSeedTier = null;
    @Nullable
    private BigInteger clientSeedTier = null;

    private int craftsCompleted = 0;
    private int clientCraftsDone = 0;

    public long clientNextZapTick;
    public int clientPulseBaseline = -1;

    public int getCraftsCompleted() {
        return this.clientCraftsDone;
    }

    private int rescanCooldown = 0;

    public CompressionCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSION_CORE.get(), pos, state);
    }

    public CompressionBand band() {
        return getBlockState().getBlock() instanceof CompressionCoreBlock core
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

    @Override
    @Nullable
    public IItemHandler portItemHandler(PortType type) {
        return switch (type) {
            case ITEM_OUT -> output;
            case ITEM_IN -> new FuelInsertHandler(this);
            case SEED -> new SeedInsertHandler(seed);
            case POWER -> null;
        };
    }

    @Override
    @Nullable
    public IBceStorage portBceHandler(PortType type) {
        return type == PortType.ITEM_IN ? bceFuelView : null;
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

    @Override
    public int getProgressMax() {
        return CompressionConfig.craftTicks(band());
    }

    @Nullable
    public BigInteger getRenderSeedTier() {
        return clientSeedTier;
    }

    public boolean isCrafting() {
        return progress > 0;
    }

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

    public static boolean isFuel(ItemStack stack) {
        return ICompressionMachine.isFuel(stack);
    }

    public static boolean isSeed(ItemStack stack) {
        return ICompressionMachine.isSeed(stack);
    }

    public void acceptFuel(ItemStack stack) {
        double massLog;
        if (stack.is(ModItems.COMPRESSED_COBBLESTONE.get())) {
            BigInteger tier = stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
            massLog = CompressionConfig.tierMassLog(tier);
        } else {
            massLog = 0.0;
        }
        massLog += Math.log10(stack.getCount());
        this.fuelPool = CompressionConfig.log10add(this.fuelPool, massLog);
        setChanged();
    }

    private boolean canStart() {
        return craftStatus() == CraftStatus.READY;
    }

    @Override
    public CraftStatus craftStatus() {
        if (this.progress > 0) {
            return CraftStatus.READY;
        }
        if (!this.formed) {
            return CraftStatus.NOT_FORMED;
        }
        BigInteger n = seedTier();
        if (n == null) {
            return CraftStatus.NO_SEED;
        }
        if (n.compareTo(band().minInputTier()) < 0) {
            return CraftStatus.SEED_TOO_LOW;
        }
        BigInteger out = nextOutputTier(n);
        if (out == null) {
            return CraftStatus.SEED_AT_CAP;
        }
        if (!this.output.insertItem(0, tierStack(out), true).isEmpty()) {
            return CraftStatus.OUTPUT_FULL;
        }
        if (!(this.fuelPool >= CompressionConfig.requiredMassLog(n))) {
            return CraftStatus.NEED_FUEL;
        }
        if (this.energy < CompressionConfig.ENERGY_PER_CRAFT) {
            return CraftStatus.NEED_ENERGY;
        }
        return CraftStatus.READY;
    }

    private void startCraft() {
        BigInteger n = seedTier();
        if (n == null) {
            return;
        }
        BigInteger out = nextOutputTier(n);
        if (out == null) {
            return;
        }
        this.pendingResult = out;
        this.renderSeedTier = n;
        this.seed.extractItem(0, 1, false);
        this.fuelPool = Double.NEGATIVE_INFINITY;
        this.progress = 1;
        setChanged();
        sync();
    }

    private void completeCraft() {
        BigInteger result = this.pendingResult;
        if (result != null) {
            output.insertItem(0, tierStack(result), false);
        }
        this.pendingResult = null;
        this.progress = 0;
        this.craftsCompleted++;
        setChanged();
        sync();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CompressionCoreBlockEntity be) {
        if (--be.rescanCooldown <= 0) {
            be.rescanCooldown = RESCAN_INTERVAL;
            be.scanRegion();
        }
        if (be.progress > 0) {
            int craftTicks = CompressionConfig.craftTicks(be.band());
            int perTick = Math.max(1, CompressionConfig.ENERGY_PER_CRAFT / craftTicks);
            be.drainEnergy(Math.min(perTick, be.energy));
            if (++be.progress >= craftTicks) {
                be.completeCraft();
            } else {
                be.setChanged();
            }
        } else if (be.canStart()) {
            be.startCraft();
        }
        be.syncRenderSeedTier();
    }

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

    @Nullable
    private BigInteger nextOutputTier(BigInteger n) {
        BigInteger cap = band().maxOutputTier();
        if (n.compareTo(cap) >= 0) {
            return null;
        }
        BigInteger doubled = n.shiftLeft(1);
        return doubled.compareTo(cap) > 0 ? cap : doubled;
    }

    public void scanRegion() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int r = band().compressorRadius();
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
                        if (!s.is(ModBlocks.FRAME.get())) {
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

    @Nullable
    public static CompressionCoreBlockEntity findCore(Level level, BlockPos from) {
        for (BlockPos off : IMultiblockPortHost.CORE_SCAN_OFFSETS) {
            BlockPos p = off.offset(from);
            if (!level.isLoaded(p)) {
                continue;
            }
            if (level.getBlockState(p).getBlock() instanceof CompressionCoreBlock
                    && level.getBlockEntity(p) instanceof CompressionCoreBlockEntity core) {
                return core;
            }
        }
        return null;
    }

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
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
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
        tag.putInt(TAG_CRAFTS_DONE, this.craftsCompleted);
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
        this.craftsCompleted = tag.getInt(TAG_CRAFTS_DONE);
        this.pendingResult = tag.contains(TAG_PENDING) ? new BigInteger(tag.getString(TAG_PENDING)) : null;
        if (this.pendingResult != null) {
            this.renderSeedTier = this.pendingResult.shiftRight(1);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_PROGRESS, progress);
        tag.putBoolean(TAG_FORMED, formed);
        tag.putDouble(TAG_FUEL_POOL, fuelPool);
        tag.putInt(TAG_CRAFTS_DONE, this.craftsCompleted);
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
        this.clientCraftsDone = tag.getInt(TAG_CRAFTS_DONE);
        this.clientSeedTier = tag.contains(TAG_RENDER_SEED) ? new BigInteger(tag.getString(TAG_RENDER_SEED)) : null;
    }

    // Live sync path: NeoForge routes the getUpdatePacket() payload here, not through handleUpdateTag (which only
    // fires on initial chunk load), so forward it to keep the client renderer current while the machine runs.
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
