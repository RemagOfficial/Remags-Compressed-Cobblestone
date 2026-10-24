package com.remag.rcc.machine;

import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.IBceStorage;
import com.remag.rcc.registry.CompressionConfig;
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

/**
 * Controller of the tier-1 <b>Generation Manipulator</b>: the 3x3x4 multiblock counterpart of the single-block
 * {@link CobblestoneGeneratorBlockEntity}. It seals one of the two interior slots of a four-tall tower (the other is
 * a 1x1 air shaft) and, while sealed by a valid shell, mints BCE straight onto the {@code 9^n} curve - from one
 * tier-1000 block's worth per second
 * ({@link GeneratorCoreTier#T1_P1000}, the tier-0 ladder's handover point) up to half a tier-{@code 10^12} block per
 * second ({@link GeneratorCoreTier#T1_HALF_BAND}). Which rung it runs on is the {@link GeneratorCoreBlock#RUNG}
 * block state, so one class covers the whole ladder exactly like {@link GeneratorTier} covers the single blocks.
 *
 * <p>Unlike the free tier-0 drip this fountain <em>costs</em> power: it burns
 * {@link GeneratorCoreTier#energyPerTick()} FE/t while minting - doubling with every rung - and stalls with empty
 * buffers when the supply cannot pay, making power infrastructure the progression gate. Minted mass lands in a
 * log10 pool capped at half a second's own output (the tier-0 stall convention: a full buffer blocks production
 * until it is drained), and the machine refuses to run unless the structure carries at least one POWER port and
 * <em>exactly one</em> BCE-out (ITEM_OUT) port - the single drain keeps the mint rate whole instead of splitting it
 * across two pipes.
 *
 * <p>It answers {@link IMultiblockPortHost} only: no GUI, no menu, no compression semantics - the interface panel
 * never resolves it as a {@link IMultiblockController}. Instead the structure <em>requires</em> exactly one
 * {@link CompressionInterfaceBlock} on a face centre as its status panel: right-clicking it prints the live rate,
 * FE and buffer readout (see {@link CompressionInterfaceBlock#useWithoutItem}), the only window into a tower whose
 * core is otherwise fully sealed. Ports forward POWER into the receive-only FE buffer and ITEM_OUT to the one-way
 * BCE source (mirroring the broken side of {@link CompressionAutoBlockEntity}'s fuel pool); ITEM_IN ports are not
 * part of this structure at all. All logic is server-authoritative.
 */
public class GeneratorCoreBlockEntity extends BlockEntity implements IMultiblockPortHost {
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_POOL = "bcePool";
    private static final String TAG_FORMED = "formed";

    private static final int RESCAN_INTERVAL = 20;

    /** FE buffer as a plain count; the exposed view is receive-only (a 0-maxExtract view would also block our drain). */
    private int energy = 0;
    /** Minted BCE in log10 space; {@link Double#NEGATIVE_INFINITY} means empty. */
    private double bcePool = Double.NEGATIVE_INFINITY;
    /** Whether a valid 3x3x4 structure currently surrounds this core; drives the mint gate. */
    private boolean formed = false;

    private int rescanCooldown = 0;

    private final IEnergyStorage energyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return GeneratorCoreBlockEntity.this.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0; // the FE that powers the fountain is never pumped back out
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

    /**
     * The minted pool as a one-way {@link IBceStorage} <em>source</em> offered on the structure's single BCE-out
     * (ITEM_OUT) port: extractable so the pipe network can drain it, and refusing every write - a generator is not
     * a sink, exactly like the tier-0 BCE rungs.
     */
    private final IBceStorage bceView = new IBceStorage() {
        @Override
        public double getBceStoredLog() {
            return GeneratorCoreBlockEntity.this.bcePool;
        }

        @Override
        public double getMaxBceLog() {
            return rung().bufferCeilingLog();
        }

        @Override
        public double receiveBce(double amountLog, boolean simulate) {
            return Double.NEGATIVE_INFINITY; // a generator is not a sink
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

    /**
     * Placing this core is what turns the tower's ports into real stores, but an FE cable or BCE pipe that already
     * knocked on one cached the empty answer and never retries - so announce the change across the whole shell box
     * (see {@link IMultiblockPortHost#invalidatePortCaps}). Without this, finishing a tower by placing the core last
     * leaves it unable to take power or give up BCE until the chunk reloads.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null) {
            IMultiblockPortHost.invalidatePortCaps(this.level, this.worldPosition);
        }
    }

    /** The rung this core runs on, read live from the block state (kept in sync by placement and {@code setBlock}). */
    public GeneratorCoreTier rung() {
        return getBlockState().getValue(GeneratorCoreBlock.RUNG);
    }

    // --- IMultiblockPortHost: what the shell ports forward -------------------------------------

    /** A tier-1 generator moves no items at all: FE in through POWER, minted BCE out through ITEM_OUT. */
    @Override
    @Nullable
    public IItemHandler portItemHandler(PortType type) {
        return null;
    }

    /** Only the single BCE-out (ITEM_OUT) port carries the pool; POWER and any stray ITEM_IN carry none. */
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

    // --- Cycle logic ---------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, GeneratorCoreBlockEntity be) {
        if (--be.rescanCooldown <= 0) {
            be.rescanCooldown = RESCAN_INTERVAL;
            be.scanRegion();
        }
        if (!be.formed) {
            return;
        }
        GeneratorCoreTier rung = be.rung();
        int cost = rung.energyPerTick();
        if (be.energy < cost) {
            return; // power starved: the fountain only flows while the grid pays for it
        }
        double ceiling = rung.bufferCeilingLog();
        double free = CompressionConfig.log10subtract(ceiling, be.bcePool);
        if (free == Double.NEGATIVE_INFINITY) {
            return; // buffer full - the single BCE-out pipe must drain it before more is minted
        }
        be.drainEnergy(cost);
        double accepted = Math.min(rung.rateLog10PerTick(), free);
        be.bcePool = Math.min(CompressionConfig.log10add(be.bcePool, accepted), ceiling);
        be.setChanged();
    }

    /** Chat readout for the interface panel (and a sneak-click on the sealed core): rate, pay, buffer, and which gate is holding the fountain. */
    public String contentsSummary() {
        GeneratorCoreTier rung = rung();
        return rung.describe() + " for " + String.format(java.util.Locale.US, "%,d", rung.energyPerTick())
                + " FE/t, buffer " + DisplayAmount.of(bcePool) + " / " + DisplayAmount.of(rung.bufferCeilingLog())
                + ", FE " + energy + " / " + rung.energyCapacity() + "; " + gateSummary(rung);
    }

    /**
     * Name the single reason the fountain is not minting right now, so a stalled tower reads as a diagnosis instead
     * of a mystery. Precedence mirrors {@link #serverTick}: a broken shell explains everything, then a power buffer
     * below one tick's bill, then a full BCE buffer waiting on its drain. With all three gates green the mint runs
     * every tick - at the top rung that bill is {@code 512,000} FE/t sustained, which ordinary cables cannot feed.
     */
    private String gateSummary(GeneratorCoreTier rung) {
        if (!formed) {
            return "structure incomplete (needs 3x3x4 shell with a 1x1 air shaft directly above or below the "
                    + "core, 1+ POWER port, exactly 1 BCE-out port and 1 interface)";
        }
        if (this.energy < rung.energyPerTick()) {
            return "starved - needs " + String.format(java.util.Locale.US, "%,d", rung.energyPerTick())
                    + " FE in the buffer and that much per tick sustained while minting";
        }
        if (CompressionConfig.log10subtract(rung.bufferCeilingLog(), this.bcePool) == Double.NEGATIVE_INFINITY) {
            return "buffer full - drain it through the BCE-out pipe";
        }
        return "minting - structure complete";
    }

    // --- Structure validation (3x3x4, ports may replace the frame) -----------------------------

    /**
     * Verify the 3x3x4 blueprint around this core and update {@code formed}. The box spans
     * {@link CompressionConfig#GEN1_REGION_RADIUS} rings horizontally and four layers tall; the core may seal
     * <em>either</em> interior slot of the centre column, and the other interior cell must be the 1x1 <em>air
     * shaft</em> - the visible well the fountain gathers in, which is what makes the column four tall. So the two
     * candidate boxes are the core low with the shaft above it, or the core high with the shaft below it (the way
     * the tower reads from the outside: core on top of the hole). The shell then follows the decompressor's ring
     * rule, stretched vertically:
     * <ul>
     *   <li>the single interior shaft cell - must be air;</li>
     *   <li>two or three boundary coordinates (edges and corners) - frame or a {@link ModBlocks#PORT port};</li>
     *   <li>exactly one boundary coordinate (face centres) - {@link ModBlocks#GLASS glass}, a port, or the
     *       {@link ModBlocks#INTERFACE interface} panel the tower needs to read its status; ITEM_IN ports are
     *       <em>not</em> part of this structure: the generator has no items to take.</li>
     * </ul>
     * The machine additionally requires at least one POWER port, <em>exactly one</em> ITEM_OUT (BCE-out) port so
     * the mint never splits across two drains, and <em>exactly one</em> interface - the same
     * {@link CompressionConfig#MIN_INTERFACES} / {@link CompressionConfig#MAX_INTERFACES} contract the compression
     * multiblocks enforce. Like the other multiblocks the check is a boolean, never a blockstate, so the core keeps
     * a single model.
     */
    public void scanRegion() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int r = CompressionConfig.GEN1_REGION_RADIUS;
        int below = CompressionConfig.GEN1_LAYERS_BELOW;
        int above = CompressionConfig.GEN1_LAYERS_ABOVE;
        // Two readings of the four-layer column: the core low with the shaft overhead, or the core high with the
        // shaft under it. Whichever box matches first forms the machine.
        this.formed = matchesStructure(-below, above, below, r)
                || matchesStructure(-above, below, -below, r);
    }

    /**
     * Check one box reading: the centre column spans {@code dyMin}..{@code dyMax} around the core and holds the
     * single air shaft cell at {@code shaftDy}. Returns true when every shell cell wears its role and the ports
     * and interface are exactly as the machine requires.
     */
    private boolean matchesStructure(int dyMin, int dyMax, int shaftDy, int r) {
        int power = 0;
        int bceOut = 0;
        int interfaces = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = dyMin; dy <= dyMax; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue; // this core
                    }
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    if (!level.isLoaded(p)) {
                        return false;
                    }
                    BlockState s = level.getBlockState(p);
                    if (dx == 0 && dz == 0 && dy == shaftDy) {
                        if (!s.isAir()) {
                            return false; // the 1x1 well is the hole - nothing may fill it
                        }
                        continue;
                    }
                    int boundary = (Math.abs(dx) == r ? 1 : 0)
                            + (Math.abs(dz) == r ? 1 : 0)
                            + (dy == dyMin || dy == dyMax ? 1 : 0);
                    if (boundary >= 2) {
                        // Edges and corners: frame, or a port standing in for it (never glass).
                        if (s.is(ModBlocks.FRAME.get())) {
                            // fine, a structural edge/corner
                        } else if (s.is(ModBlocks.PORT.get())) {
                            switch (s.getValue(CompressionPortBlock.PORT_TYPE)) {
                                case POWER -> power++;
                                case ITEM_OUT -> bceOut++;
                                case ITEM_IN -> {
                                    return false; // this machine takes no items
                                }
                            }
                        } else {
                            return false;
                        }
                    } else if (boundary == 1) {
                        // Face centres: glass, a port, or the status interface.
                        if (s.is(ModBlocks.GLASS.get())) {
                            // fine, a viewing face
                        } else if (s.is(ModBlocks.INTERFACE.get())) {
                            interfaces++;
                        } else if (s.is(ModBlocks.PORT.get())) {
                            switch (s.getValue(CompressionPortBlock.PORT_TYPE)) {
                                case POWER -> power++;
                                case ITEM_OUT -> bceOut++;
                                case ITEM_IN -> {
                                    return false; // this machine takes no items
                                }
                            }
                        } else {
                            return false;
                        }
                    } else {
                        return false; // an unexpected interior cell (core and shaft are the only two)
                    }
                }
            }
        }
        return power >= CompressionConfig.MIN_GEN1_POWER_PORTS
                && bceOut == CompressionConfig.EXACT_GEN1_BCE_OUT_PORTS
                && interfaces >= CompressionConfig.MIN_INTERFACES
                && interfaces <= CompressionConfig.MAX_INTERFACES;
    }

    // --- Persistence ---------------------------------------------------------------------------

    /**
     * A broken core must come back as the rung it was, not the default state - the {@code RUNG} rides the
     * {@code block_state} component through the loot table's {@code copy_components} and back through
     * {@link GeneratorCoreBlock#getStateForPlacement} (the {@link BcePipeBlockEntity} route).
     */
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.energy = tag.getInt(TAG_ENERGY);
        this.bcePool = tag.contains(TAG_POOL) ? tag.getDouble(TAG_POOL) : Double.NEGATIVE_INFINITY;
        this.formed = tag.getBoolean(TAG_FORMED);
    }

    /** Compact {@code 10^N} form of a log10 mass for the chat readout (mirrors the single generator's helper). */
    private static final class DisplayAmount {
        static String of(double log) {
            if (log == Double.NEGATIVE_INFINITY || Double.isNaN(log) || log <= 0.0) {
                return "0";
            }
            if (Math.abs(log - Math.rint(log)) < 1e-3) {
                return "10^" + (long) Math.rint(log);
            }
            return String.format(java.util.Locale.US, "10^%.1f", log);
        }
    }
}
