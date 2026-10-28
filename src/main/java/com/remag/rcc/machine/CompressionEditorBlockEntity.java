package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.menu.CompressionEditorMenu;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.items.ItemStackHandler;

import java.math.BigInteger;

public class CompressionEditorBlockEntity extends BlockEntity implements MenuProvider {
    private static final String TAG_SLOT = "slot";
    private static final String TAG_EXPONENT = "exponent";

    public static final int MAX_EXPONENT =
            CompressionBand.TOP.maxOutputTier().toString().length() - 1;

    private final ItemStackHandler slot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slotIndex, ItemStack stack) {
            return isCobblestone(stack);
        }
    };

    private int exponent = 0;

    public CompressionEditorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSION_EDITOR.get(), pos, state);
    }

    public static boolean isCobblestone(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(Items.COBBLESTONE) || stack.is(ModItems.COMPRESSED_COBBLESTONE.get()));
    }

    public ItemStackHandler getSlot() {
        return slot;
    }

    public int getExponent() {
        return exponent;
    }

    public void setExponent(int value) {
        int clamped = Math.max(0, Math.min(MAX_EXPONENT, value));
        if (clamped != this.exponent) {
            this.exponent = clamped;
            setChanged();
        }
    }

    public BigInteger targetTier() {
        return BigInteger.TEN.pow(exponent);
    }

    public void applyTier() {
        ItemStack stack = slot.getStackInSlot(0);
        if (stack.isEmpty() || !isCobblestone(stack)) {
            return;
        }
        BigInteger target = targetTier();
        if (stack.is(Items.COBBLESTONE)) {
            ItemStack result = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
            result.set(ModDataComponents.COMPRESSION_LEVEL.get(), target);
            result.setCount(stack.getCount());
            slot.setStackInSlot(0, result);
            setChanged();
            return;
        }
        BigInteger current = stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
        if (!target.equals(current)) {
            stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), target);
            setChanged();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CompressionEditorBlockEntity be) {
        be.applyTier();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rcc.compression_editor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CompressionEditorMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TAG_SLOT, slot.serializeNBT(registries));
        tag.putInt(TAG_EXPONENT, exponent);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_SLOT)) {
            slot.deserializeNBT(registries, tag.getCompound(TAG_SLOT));
        }
        this.exponent = Math.max(0, Math.min(MAX_EXPONENT, tag.getInt(TAG_EXPONENT)));
    }
}
