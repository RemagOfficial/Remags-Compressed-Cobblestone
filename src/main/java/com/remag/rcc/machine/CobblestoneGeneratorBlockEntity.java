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

public class CobblestoneGeneratorBlockEntity extends BlockEntity implements IGeneratorArrayMember {
    private static final String TAG_TIER = "tier";
    private static final String TAG_OUTPUT = "output";
    private static final String TAG_POOL = "bcePool";
    private static final String TAG_CARRY = "itemCarry";
    private static final String TAG_MINT_COUNTDOWN = "mintCountdown";

    private GeneratorTier tier = GeneratorTier.BASE;

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

    private double itemCarry = 0.0;

    private double bcePool = Double.NEGATIVE_INFINITY;

    private int mintCountdown = CompressionConfig.GEN_MINT_PERIOD_TICKS;

    @Nullable
    private BceNetwork bus = null;

    private final IBceStorage bceView = new IBceStorage() {
        @Override
        public double getBceStoredLog() {
            return CobblestoneGeneratorBlockEntity.this.bcePool;
        }

        @Override
        public double getMaxBceLog() {
            return CompressionConfig.generatorLumpCeilingLog(lumpRateLog());
        }

        @Override
        public double receiveBce(double amountLog, boolean simulate) {
            return Double.NEGATIVE_INFINITY;
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

    public IBceStorage getBceStorage(@Nullable Direction side) {
        return tier.outputIsItems() ? null : bceView;
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return tier.outputIsItems() ? output : null;
    }

    public GeneratorTier getTier() {
        return tier;
    }

    public void onTierChanged(BlockState state) {
        this.tier = readTier(state);
        setChanged();
    }

    private static GeneratorTier readTier(BlockState state) {
        return state.getOptionalValue(CobblestoneGeneratorBlock.TIER).orElse(GeneratorTier.BASE);
    }

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

    @Override
    @Nullable
    public BigInteger arrayShareExponent() {
        return tier.outputIsItems() ? null : tier.shareExponent();
    }

    @Override
    public CompressionBand arrayBand() {
        return tier.band();
    }

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

    public static void serverTick(Level level, BlockPos pos, BlockState state, CobblestoneGeneratorBlockEntity be) {
        if (be.tier.outputIsItems()) {
            be.produceItems();
        } else {
            be.produceBce(level);
        }
    }

    private void produceItems() {
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
            itemCarry -= accepted;
            setChanged();
        }
    }

    private void produceBce(Level level) {
        if (--this.mintCountdown > 0) {
            return;
        }
        this.mintCountdown = CompressionConfig.GEN_MINT_PERIOD_TICKS;
        this.bus = BceNetwork.forGenerator(level, this.worldPosition);
        BceNetwork bus = this.bus;
        boolean lead = bus == null || bus.isGeneratorLead(this.worldPosition);
        if (!lead || this.bcePool != Double.NEGATIVE_INFINITY) {
            return;
        }
        double lumpLog = lumpRateLog();
        if (lumpLog == Double.NEGATIVE_INFINITY) {
            return;
        }
        this.bcePool = lumpLog;
        setChanged();
    }

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
        this.mintCountdown = tag.contains(TAG_MINT_COUNTDOWN)
                ? tag.getInt(TAG_MINT_COUNTDOWN)
                : CompressionConfig.GEN_MINT_PERIOD_TICKS;
    }
}
