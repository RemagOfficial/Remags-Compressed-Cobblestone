package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.BceNetwork;
import com.remag.rcc.energy.IBceStorage;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

/**
 * The beating heart of a {@link CobblestoneGeneratorBlock}: an endless, fuel-free source of base cobblestone that is
 * the entry point of the whole compression economy. Which rung it runs on comes from the block's {@link GeneratorTier}
 * state, so one class covers the ladder.
 *
 * <p>Each {@link CompressionConfig#GEN_MINT_PERIOD_TICKS}-tick period it produces its tier's output and, depending on
 * that tier's mode, lands it in one of two places:
 *
 * <ul>
 *   <li><b>item rungs</b> ({@link GeneratorTier#outputIsItems()} - the {@link GeneratorTier#BASE} drip) accumulate a
 *       whole-cobblestone count every tick and push it into a one-slot {@link ItemStackHandler} output. The output is
 *       exposed as the standard {@code Capabilities.ItemHandler.BLOCK}, so a hopper or pipe on any face carries it away;
 *       when the slot is full, generation stalls (the physical limit that makes a low generator a gate).</li>
 *   <li><b>BCE rungs</b> mint one {@link CompressionConfig#generatorLumpCeilingLog lump} into a log10 pool and
 *       expose it as a one-way {@link IBceStorage} <em>source</em> ({@code canExtract} true, {@code canReceive} false)
 *       on the {@code rcc:bce} capability, which a pipe's extract end drains. A lump still in the pool stalls the next
 *       mint until a pipe carries it off - the deliberate progression pressure. This is the same lump cadence the
 *       {@code MASTER_PLAN.md} D23 array rule uses (see {@link GeneratorCoreBlockEntity}): these rungs are band 0, so
 *       each contributes its full {@link GeneratorTier#shareExponent() share} and two {@code 9^500} blocks on one bus
 *       mint a whole tier-1000 block per second.</li>
 * </ul>
 *
 * <p>Unlike the compressor/decompressor this machine has no GUI, no energy cost, and no seed slot: it exists purely to
 * mint mass. Its economics are the mirror image of the {@link CompressionAutoBlockEntity}'s fuel pool - where that
 * sinks BCE ({@code canExtract} false), this is a source ({@code canReceive} false).
 */
public class CobblestoneGeneratorBlockEntity extends BlockEntity implements IGeneratorArrayMember {
    private static final String TAG_TIER = "tier";
    private static final String TAG_OUTPUT = "output";
    private static final String TAG_POOL = "bcePool";
    private static final String TAG_CARRY = "itemCarry";
    private static final String TAG_MINT_COUNTDOWN = "mintCountdown";

    private GeneratorTier tier = GeneratorTier.BASE;

    /** Item-rung output: one stack of vanilla cobblestone, extracted by hoppers/pipes on any face. */
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Items.COBBLESTONE);
        }
    };

    /** Fraction of a cobblestone carried to the next tick for item rungs whose per-tick rate is below one (the 40/sec drip). */
    private double itemCarry = 0.0;

    /** BCE-rung buffer as a log10 mass; {@link Double#NEGATIVE_INFINITY} means empty. */
    private double bcePool = Double.NEGATIVE_INFINITY;

    /** Ticks until the next BCE lump; resets to {@link CompressionConfig#GEN_MINT_PERIOD_TICKS} after each attempt. */
    private int mintCountdown = CompressionConfig.GEN_MINT_PERIOD_TICKS;

    /** The bus this generator drains into, re-found each mint period; {@code null} when solo (no extract pipe). */
    @Nullable
    private BceNetwork bus = null;

    /** The one-way BCE source a pipe's extract end sees; receive is refused because a generator only ever makes BCE. */
    private final IBceStorage bceView = new IBceStorage() {
        @Override
        public double getBceStoredLog() {
            return CobblestoneGeneratorBlockEntity.this.bcePool;
        }

        @Override
        public double getMaxBceLog() {
            // A whole lump's ceiling: the pool holds exactly one array/solo lump, never a per-tick slice.
            return CompressionConfig.generatorLumpCeilingLog(lumpRateLog());
        }

        @Override
        public double receiveBce(double amountLog, boolean simulate) {
            return Double.NEGATIVE_INFINITY; // a generator is not a sink
        }

        @Override
        public double extractBce(double amountLog, boolean simulate) {
            double pool = CobblestoneGeneratorBlockEntity.this.bcePool;
            if (pool == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            double take = Math.min(amountLog, pool);
            if (!simulate) {
                CobblestoneGeneratorBlockEntity.this.bcePool = CompressionConfig.log10subtract(pool, take);
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

    public CobblestoneGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COBBLESTONE_GENERATOR.get(), pos, state);
        this.tier = readTier(state);
    }

    // --- Capability accessors ------------------------------------------------------------------

    public IBceStorage getBceStorage(@Nullable Direction side) {
        return tier.outputIsItems() ? null : bceView; // item rungs answer nothing on the BCE cap
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return tier.outputIsItems() ? output : null; // BCE rungs answer nothing on the item cap
    }

    public GeneratorTier getTier() {
        return tier;
    }

    /** Re-read the rung after a block-state change (e.g. sneak-clicking to cycle tiers for testing). */
    public void onTierChanged(BlockState state) {
        this.tier = readTier(state);
        setChanged();
    }

    private static GeneratorTier readTier(BlockState state) {
        return state.getOptionalValue(CobblestoneGeneratorBlock.TIER).orElse(GeneratorTier.BASE);
    }

    /** Readout for the current buffer/output, for the sneak-right-click chat message. */
    public String contentsSummary() {
        if (tier.outputIsItems()) {
            return tier.describe() + ", buffered " + output.getStackInSlot(0).getCount() + " cobblestone";
        }
        BceNetwork bus = this.bus;
        int members = bus == null ? 1 : bus.arraySize();
        long wanted = bus == null ? 0L : bus.arrayFullOutputTowers();
        String arrayText = bus == null
                ? "solo (no BCE bus on it)"
                : "array " + members + (wanted > 0L ? " / " + wanted + " blocks" : " blocks");
        return tier.describe() + " share, " + arrayText + "; bus total "
                + DisplayNames.bceMagnitude(lumpRateLog()) + "/sec, buffer "
                + DisplayNames.bceMagnitude(bcePool) + " / " + DisplayNames.bceMagnitude(bceView.getMaxBceLog());
    }

    // --- IGeneratorArrayMember (D23) -----------------------------------------------------------

    /**
     * This block's share of its bus: a BCE rung always contributes its full {@link GeneratorTier#shareExponent()}
     * (band 0 divides nothing), while {@link GeneratorTier#outputIsItems() item rungs} mint physical cobblestone and
     * are never counted, so they return {@code null} and leave the array total alone.
     */
    @Override
    @Nullable
    public BigInteger arrayShareExponent() {
        return tier.outputIsItems() ? null : tier.shareExponent();
    }

    @Override
    public CompressionBand arrayBand() {
        return tier.band();
    }

    /** log10 BCE/sec of the lump this block would mint: the whole bus's aggregate when it is on one, else its solo share. */
    private double lumpRateLog() {
        BceNetwork bus = this.bus;
        if (bus != null) {
            double aggregate = bus.arrayRateLog10PerSec();
            if (aggregate != Double.NEGATIVE_INFINITY) {
                return aggregate;
            }
        }
        return tier.rateLog10PerSec();
    }

    // --- Cycle logic ---------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, CobblestoneGeneratorBlockEntity be) {
        if (be.tier.outputIsItems()) {
            be.produceItems();
        } else {
            be.produceBce(level);
        }
    }

    /** Item rung: add this tick's whole-cobblestone yield (with a fractional carry) into the output slot; stall when full. */
    private void produceItems() {
        // BASE is a fixed whole-count-per-second drip; the (rare) 9^n item rung would be astronomically fast, so the
        // per-tick rate is derived from the per-second figure and clamped into an int the slot can hold.
        double perTick = Math.pow(10.0, tier.rateLog10PerSec() - Math.log10(CompressionConfig.GEN_MINT_PERIOD_TICKS));
        itemCarry += perTick;
        int whole = (int) Math.min(itemCarry, (double) Integer.MAX_VALUE);
        if (whole <= 0) {
            return;
        }
        ItemStack stack = new ItemStack(Items.COBBLESTONE);
        stack.setCount(whole);
        ItemStack leftover = output.insertItem(0, stack, false);
        int accepted = whole - leftover.getCount();
        if (accepted > 0) {
            itemCarry -= accepted; // only pay for what physically landed; a full slot backs the mass up as carry
            setChanged();
        }
    }

    /**
     * BCE rung: once per {@link CompressionConfig#GEN_MINT_PERIOD_TICKS} mint one lump - the whole bus's aggregate
     * when an extract pipe is attached, else this block's solo share - and only into an empty pool (the stall
     * convention: an undrained lump refuses the next second rather than banking it). Only the bus's elected lead
     * deposits; a non-lead block still counts its share toward the total that the lead mints.
     */
    private void produceBce(Level level) {
        if (--this.mintCountdown > 0) {
            return;
        }
        this.mintCountdown = CompressionConfig.GEN_MINT_PERIOD_TICKS;
        this.bus = BceNetwork.forGenerator(level, this.worldPosition);
        BceNetwork bus = this.bus;
        boolean lead = bus == null || bus.isGeneratorLead(this.worldPosition);
        if (!lead || this.bcePool != Double.NEGATIVE_INFINITY) {
            return; // upkeep-only member, or last lump has not drained yet
        }
        double lumpLog = lumpRateLog();
        if (lumpLog == Double.NEGATIVE_INFINITY) {
            return;
        }
        this.bcePool = lumpLog;
        setChanged();
    }

    // --- Persistence ---------------------------------------------------------------------------

    /**
     * A broken generator must come back as the rung it actually was, not the block's default {@link GeneratorTier#BASE}
     * state - so the tier is published here as the {@code block_state} component that the loot table's
     * {@code copy_components} reads off this block entity and {@link CobblestoneGeneratorBlock#getStateForPlacement}
     * reads back on re-placement (the same route as {@link BcePipeBlockEntity}). Reads the live block state (kept in
     * sync by {@code setBlock}) rather than the constructor snapshot.
     */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        builder.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY
                .with(CobblestoneGeneratorBlock.TIER, getBlockState().getValue(CobblestoneGeneratorBlock.TIER)));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(TAG_TIER, tier.getSerializedName());
        tag.put(TAG_OUTPUT, output.serializeNBT(registries));
        tag.putDouble(TAG_POOL, bcePool);
        tag.putDouble(TAG_CARRY, itemCarry);
        tag.putInt(TAG_MINT_COUNTDOWN, mintCountdown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        GeneratorTier loaded = GeneratorTier.byName(tag.getString(TAG_TIER));
        this.tier = loaded == null ? readTier(getBlockState()) : loaded;
        if (tag.contains(TAG_OUTPUT)) {
            output.deserializeNBT(registries, tag.getCompound(TAG_OUTPUT));
        }
        this.bcePool = tag.contains(TAG_POOL) ? tag.getDouble(TAG_POOL) : Double.NEGATIVE_INFINITY;
        this.itemCarry = tag.getDouble(TAG_CARRY);
        // Absent on pre-array saves: start a full period out so an old generator just resumes minting.
        this.mintCountdown = tag.contains(TAG_MINT_COUNTDOWN)
                ? tag.getInt(TAG_MINT_COUNTDOWN)
                : CompressionConfig.GEN_MINT_PERIOD_TICKS;
    }
}
