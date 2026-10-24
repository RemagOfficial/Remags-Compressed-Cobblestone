package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.IBceStorage;
import com.remag.rcc.registry.CompressionConfig;
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

/**
 * The beating heart of a {@link CobblestoneGeneratorBlock}: an endless, fuel-free source of base cobblestone that is
 * the entry point of the whole compression economy. Which rung it runs on comes from the block's {@link GeneratorTier}
 * state, so one class covers the ladder.
 *
 * <p>Each server tick it adds its tier's output and, depending on that tier's mode, lands it in one of two places:
 *
 * <ul>
 *   <li><b>item rungs</b> ({@link GeneratorTier#outputIsItems()} - the {@link GeneratorTier#BASE} drip) accumulate a
 *       whole-cobblestone count and push it into a one-slot {@link ItemStackHandler} output. The output is exposed as
 *       the standard {@code Capabilities.ItemHandler.BLOCK}, so a hopper or pipe on any face carries it away; when the
 *       slot is full, generation stalls (the physical limit that makes a low generator a gate).</li>
 *   <li><b>BCE rungs</b> accumulate the mass into a log10 pool capped at {@link GeneratorTier#bufferCeilingLog()} and
 *       expose it as a one-way {@link IBceStorage} <em>source</em> ({@code canExtract} true, {@code canReceive} false)
 *       on the {@code rcc:bce} capability, which a pipe's extract end drains. A full buffer stalls generation until a
 *       pipe carries the BCE off - the deliberate progression pressure.</li>
 * </ul>
 *
 * <p>Unlike the compressor/decompressor this machine has no GUI, no energy cost, and no seed slot: it exists purely to
 * mint mass. Its economics are the mirror image of the {@link CompressionAutoBlockEntity}'s fuel pool - where that
 * sinks BCE ({@code canExtract} false), this is a source ({@code canReceive} false).
 */
public class CobblestoneGeneratorBlockEntity extends BlockEntity {
    private static final String TAG_TIER = "tier";
    private static final String TAG_OUTPUT = "output";
    private static final String TAG_POOL = "bcePool";
    private static final String TAG_CARRY = "itemCarry";

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

    /** The one-way BCE source a pipe's extract end sees; receive is refused because a generator only ever makes BCE. */
    private final IBceStorage bceView = new IBceStorage() {
        @Override
        public double getBceStoredLog() {
            return CobblestoneGeneratorBlockEntity.this.bcePool;
        }

        @Override
        public double getMaxBceLog() {
            return CobblestoneGeneratorBlockEntity.this.tier.bufferCeilingLog();
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
        return tier.describe() + ", buffer " + DisplayAmount.of(bcePool) + " / "
                + DisplayAmount.of(tier.bufferCeilingLog());
    }

    // --- Cycle logic ---------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, CobblestoneGeneratorBlockEntity be) {
        if (be.tier.outputIsItems()) {
            be.produceItems();
        } else {
            be.produceBce();
        }
    }

    /** Item rung: add this tick's whole-cobblestone yield (with a fractional carry) into the output slot; stall when full. */
    private void produceItems() {
        // BASE is a fixed whole-count-per-second drip; the (rare) 9^n item rung would be astronomically fast, so the
        // per-tick rate is derived in log space and clamped into an int the slot can hold.
        double perTick = Math.pow(10.0, tier.rateLog10PerTick());
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

    /** BCE rung: add the per-tick log10 mass into the pool, capped at the tier's buffer ceiling; stall when full. */
    private void produceBce() {
        double ceiling = tier.bufferCeilingLog();
        double free = CompressionConfig.log10subtract(ceiling, bcePool);
        if (free == Double.NEGATIVE_INFINITY) {
            return; // buffer full - a pipe must drain it before more can be made
        }
        double accepted = Math.min(tier.rateLog10PerTick(), free);
        bcePool = Math.min(CompressionConfig.log10add(bcePool, accepted), ceiling);
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
    }

    /** Compact {@code 10^N} form of a log10 mass for the chat readout (mirrors the GUI's display helper). */
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
