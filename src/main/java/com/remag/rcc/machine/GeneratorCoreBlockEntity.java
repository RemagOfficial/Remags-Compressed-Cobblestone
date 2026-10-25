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
 * buffers when the supply cannot pay, making power infrastructure the progression gate. It is also one
 * <em>member</em> of a {@code MASTER_PLAN.md} D23 generator array: what it deposits alone is only its rung's
 * {@link GeneratorCoreTier#shareExponent() share} (the rung exponent divided by {@code 2^band}), and
 * {@link CompressionBand#arrayFullOutputTowers() 2^(band+1)} top-rung towers joined on one BCE bus mint a whole
 * band-max block per second. The mint is therefore <b>one lump per second</b> ({@link
 * CompressionConfig#GEN_MINT_PERIOD_TICKS} ticks), not twenty per-tick slices: a slice is below the {@code double}'s
 * resolution at these magnitudes and would silently round to nothing. Only the bus's elected {@link
 * BceNetwork#isGeneratorLead lead} tower deposits the {@code 9^arraySum} lump, and only into an <em>empty</em> pool -
 * the tier-0 stall convention, now lump-shaped - while every formed, powered tower drains its own FE/t as upkeep
 * whether or not it leads. A tower whose BCE-out port touches no extract pipe falls back to minting its own solo
 * share, so a lone tower's readout is never zero.
 *
 * <p>It answers {@link IMultiblockPortHost} and {@link IGeneratorArrayMember} only: no GUI, no menu, no compression semantics - the interface panel
 * never resolves it as a {@link IMultiblockController}. Instead the structure <em>requires</em> exactly one
 * {@link CompressionInterfaceBlock} on a face centre as its status panel: right-clicking it prints the live rate,
 * FE and buffer readout (see {@link CompressionInterfaceBlock#useWithoutItem}), the only window into a tower whose
 * core is otherwise fully sealed. Ports forward POWER into the receive-only FE buffer and ITEM_OUT to the one-way
 * BCE source (mirroring the broken side of {@link CompressionAutoBlockEntity}'s fuel pool); ITEM_IN ports are not
 * part of this structure at all. All logic is server-authoritative.
 */
public class GeneratorCoreBlockEntity extends BlockEntity implements IMultiblockPortHost, IGeneratorArrayMember {
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_POOL = "bcePool";
    private static final String TAG_FORMED = "formed";
    private static final String TAG_MINT_COUNTDOWN = "mintCountdown";

    private static final int RESCAN_INTERVAL = CompressionConfig.GEN_MINT_PERIOD_TICKS;

    /** FE buffer as a plain count; the exposed view is receive-only (a 0-maxExtract view would also block our drain). */
    private int energy = 0;
    /** Minted BCE in log10 space; {@link Double#NEGATIVE_INFINITY} means empty. */
    private double bcePool = Double.NEGATIVE_INFINITY;
    /** Whether a valid 3x3x4 structure currently surrounds this core; drives the mint gate. */
    private boolean formed = false;
    /** Ticks until the next lump attempt; resets to {@link CompressionConfig#GEN_MINT_PERIOD_TICKS} after each. */
    private int mintCountdown = CompressionConfig.GEN_MINT_PERIOD_TICKS;

    private int rescanCooldown = 0;
    /** The single BCE-out (ITEM_OUT) port's position, found by the structure scan; {@code null} when not formed. */
    @Nullable
    private BlockPos bceOutPos = null;
    /** The bus this tower's BCE-out port drains into, re-found on each rescan; {@code null} when solo (no pipe). */
    @Nullable
    private BceNetwork bus = null;
    /**
     * Every POWER-port cell this tower claims inside its own shell, republished on each successful {@link
     * #matchesStructure} and cleared when the tower breaks. A port standing on a wall shared with a same-band
     * neighbour lands in <em>both</em> cores' lists, which is what lets {@link CompressionPortBlockEntity} split that
     * port's incoming FE across every co-owner (see {@link #claimsPowerPort(BlockPos)}).
     */
    private List<BlockPos> powerPorts = List.of();
    /** The POWER ports found by the currently-running {@link #matchesStructure} pass; published to {@link #powerPorts} on a match. */
    private final List<BlockPos> matchedPowerPorts = new ArrayList<>();

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
            // A whole lump's ceiling: the buffer holds exactly one band/array lump, never a partial slice.
            return CompressionConfig.generatorLumpCeilingLog(lumpRateLog());
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
            IMultiblockPortHost.invalidatePortCaps(this.level, this.worldPosition, this.portShellExtent());
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

    // --- IGeneratorArrayMember (D23) -----------------------------------------------------------

    /**
     * This tower's share of its bus while it is actually producing: its rung's {@link
     * GeneratorCoreTier#shareExponent() share exponent}, or {@code null} when the shell is incomplete or the FE
     * buffer cannot pay a tick's upkeep, so a starved or broken tower drops out of the array total rather than
     * inflating it (see {@link com.remag.rcc.energy.BceNetwork}).
     */
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

    /**
     * The log10 BCE/sec figure of the lump this tower would deposit: the whole bus's aggregate when it is on one and
     * producing, else its own solo share. Used both to size the mint and to report the buffer ceiling.
     */
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

    // --- Cycle logic ---------------------------------------------------------------------------

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
        // Upkeep: every formed member pays its own FE/t every tick, whether or not it is the array's lead.
        if (be.energy < cost) {
            return; // power starved: the fountain only flows while the grid pays for it
        }
        be.drainEnergy(cost);
        if (--be.mintCountdown > 0) {
            return;
        }
        be.mintCountdown = CompressionConfig.GEN_MINT_PERIOD_TICKS;
        be.mintLump();
    }

    /**
     * Deposit one {@code 9^arraySum} lump, once per second. Only the bus's elected lead mints, and only into an empty
     * pool (the stall convention: an undrained lump means this second's output is refused, not banked). A solo tower
     * (no extract pipe on its BCE-out port) is its own lead and mints its own share.
     */
    private void mintLump() {
        BceNetwork bus = this.bus;
        boolean lead = bus == null || bus.isGeneratorLead(this.worldPosition);
        if (!lead || this.bcePool != Double.NEGATIVE_INFINITY) {
            return; // upkeep only, or last second's lump has not drained yet
        }
        double lumpLog = lumpRateLog();
        if (lumpLog == Double.NEGATIVE_INFINITY) {
            return;
        }
        // The lump fits its own ceiling by construction (generatorLumpCeilingLog adds headroom above it), so the
        // deposit is unconditional once the pool is empty and this tower leads.
        this.bcePool = lumpLog;
        this.setChanged();
    }

    /** Chat readout for the interface panel (and a sneak-click on the sealed core): share, array, bus total, lead, FE, buffer, gate. */
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

    /** Name this tower's role in its array: the lead that mints, a upkeep-only member, or a lone producer. */
    private String leadWord(@Nullable BceNetwork bus) {
        if (bus == null) {
            return "minting solo";
        }
        return bus.isGeneratorLead(this.worldPosition) ? "lead (mints the lump)" : "upkeep only";
    }

    /**
     * Name the single reason this tower is not depositing a lump right now, so a stalled array reads as a diagnosis
     * instead of a mystery. Precedence mirrors {@link #serverTick}: a broken shell explains everything, then a power
     * buffer below one tick's upkeep, then an undrained lump waiting on its pipe, then the array role - a member that
     * is not the lead never deposits, and an array that has passed its band's ceiling reports the clamp.
     */
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
                return "array full - clamped at this band's ceiling";
            }
            if (!bus.isGeneratorLead(this.worldPosition)) {
                return "not the lead - this tower is upkeep only";
            }
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
        // Snapshot the old claim set so we can tell the shell's POWER ports (and any cable wired to a shared wall)
        // to re-resolve their owner count when this tower starts or stops co-owning one.
        List<BlockPos> oldPorts = this.powerPorts;
        this.bceOutPos = null;
        this.powerPorts = List.of();
        // Two readings of the four-layer column: the core low with the shaft overhead, or the core high with the
        // shaft under it. Whichever box matches first forms the machine; the match that wins leaves its BCE-out port
        // position and POWER-port list in {@link #matchedBceOutPos}/{@link #matchedPowerPorts} for the tick and the
        // port splitter to read from.
        this.formed = matchesStructure(-below, above, below, r)
                || matchesStructure(-above, below, -below, r);
        if (!this.formed) {
            this.bceOutPos = null;
            this.powerPorts = List.of();
        }
        invalidateChangedPowerPorts(oldPorts, this.powerPorts);
    }

    /**
     * Ask the level to drop any cached capability answer at every POWER-port cell whose ownership this scan changed,
     * so a cable sitting on a wall shared with a neighbour re-queries and now sees the new co-owner (or loses one when
     * a neighbour tower breaks). Skipped entirely when the claim set is unchanged, so the 20-tick rescan never churns
     * the caches of a stable array. The shared port lives inside both towers' shells, so each core invalidating its own
     * claimed ports covers the seam from both sides however the two towers finish assembling.
     */
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

    /**
     * Whether this tower claims {@code cell} as one of its POWER ports while formed. Two same-band towers whose shells
     * overlap on a shared wall both claim the port standing there, which is how {@link CompressionPortBlockEntity}
     * discovers the co-owners to split a shared port's incoming FE between.
     */
    public boolean claimsPowerPort(BlockPos cell) {
        return this.formed && this.powerPorts.contains(cell);
    }

    /** Read-only view of this tower's claimed POWER-port cells (empty when not formed). */
    public List<BlockPos> claimedPowerPorts() {
        return this.powerPorts;
    }

    /** The BCE-out port found by the last {@link #matchesStructure} call; becomes {@link #bceOutPos} only when it forms. */
    @Nullable
    private BlockPos matchedBceOutPos = null;

    /**
     * Check one box reading: the centre column spans {@code dyMin}..{@code dyMax} around the core and holds the
     * single air shaft cell at {@code shaftDy}. Returns true when every shell cell wears its role and the ports
     * and interface are exactly as the machine requires.
     *
     * <p>Wall sharing (see {@code MASTER_PLAN.md} D24): a shell cell that also lies inside an adjacent <em>same-band</em>
     * tower's box is a <em>shared</em> cell. On such a cell frame, glass and a {@link PortType#POWER} port are all
     * still valid, and the POWER port is recorded as co-owned so its incoming FE is later split between the towers;
     * the {@link PortType#ITEM_OUT} drain and the status {@link ModBlocks#INTERFACE interface} are refused there, so
     * every tower keeps one private drain and one private panel. The neighbour test reads the block at the shared
     * wall's far side, so two cores 2r apart agree on the seam the moment both are placed (whether or not either has
     * completed its own scan).
     */
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
                    boolean shared = isSharedCell(dx, dz, r);
                    if (boundary >= 2) {
                        // Edges and corners: frame, or a port standing in for it (never glass).
                        if (s.is(ModBlocks.FRAME.get())) {
                            // fine, a structural edge/corner
                        } else if (s.is(ModBlocks.PORT.get())) {
                            PortType type = s.getValue(CompressionPortBlock.PORT_TYPE);
                            if (shared && type != PortType.POWER) {
                                return false; // only POWER ports may stand on a shared wall
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
                            if (shared) {
                                return false; // every tower keeps its own private status panel
                            }
                            interfaces++;
                        } else if (s.is(ModBlocks.PORT.get())) {
                            PortType type = s.getValue(CompressionPortBlock.PORT_TYPE);
                            if (shared && type != PortType.POWER) {
                                return false; // only POWER ports may stand on a shared wall
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
        boolean ok = power >= CompressionConfig.MIN_GEN1_POWER_PORTS
                && bceOut == CompressionConfig.EXACT_GEN1_BCE_OUT_PORTS
                && interfaces >= CompressionConfig.MIN_INTERFACES
                && interfaces <= CompressionConfig.MAX_INTERFACES
                && this.matchedBceOutPos != null;
        if (ok) {
            // Publish this reading's drains and co-owned POWER ports; cleared again by scanRegion when the tower breaks.
            this.bceOutPos = this.matchedBceOutPos;
            this.powerPorts = List.copyOf(this.matchedPowerPorts);
        }
        return ok;
    }

    /**
     * Whether the shell cell at offset {@code (dx, dz)} sits on a wall shared with an adjacent same-band tower: it
     * must be on this tower's extreme face along some horizontal axis <em>and</em> have a same-band generator core
     * {@code 2r} out along that axis, whose box therefore covers the very same cell. Top and bottom faces ({@code dy}
     * extremes) are never reported shared - towers do not stack - which is what guarantees each tower a private face
     * for its BCE-out port and interface.
     */
    private boolean isSharedCell(int dx, int dz, int r) {
        if (Math.abs(dx) == r && sameBandCoreAt(worldPosition.offset(Integer.signum(dx) * 2 * r, 0, 0))) {
            return true;
        }
        return Math.abs(dz) == r && sameBandCoreAt(worldPosition.offset(0, 0, Integer.signum(dz) * 2 * r));
    }

    /** True when {@code p} holds a {@link ModBlocks#GENERATOR_CORE generator core} running on this tower's band. */
    private boolean sameBandCoreAt(BlockPos p) {
        if (!level.isLoaded(p)) {
            return false;
        }
        BlockState s = level.getBlockState(p);
        return s.is(ModBlocks.GENERATOR_CORE.get())
                && s.getValue(GeneratorCoreBlock.RUNG).band() == this.rung().band();
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
        tag.putInt(TAG_MINT_COUNTDOWN, mintCountdown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.energy = tag.getInt(TAG_ENERGY);
        this.bcePool = tag.contains(TAG_POOL) ? tag.getDouble(TAG_POOL) : Double.NEGATIVE_INFINITY;
        this.formed = tag.getBoolean(TAG_FORMED);
        // Absent on pre-array saves: start a full period out so an old tower just resumes minting.
        this.mintCountdown = tag.contains(TAG_MINT_COUNTDOWN)
                ? tag.getInt(TAG_MINT_COUNTDOWN)
                : CompressionConfig.GEN_MINT_PERIOD_TICKS;
    }
}
