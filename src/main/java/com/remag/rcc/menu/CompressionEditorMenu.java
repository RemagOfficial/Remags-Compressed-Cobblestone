package com.remag.rcc.menu;

import com.remag.rcc.machine.CompressionEditorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class CompressionEditorMenu extends AbstractContainerMenu implements ContainerData {
    private static final int MACHINE_INDEX = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private static final int DATA_COUNT = 1;
    private static final int DATA_EXPONENT = 0;

    @Nullable
    private final CompressionEditorBlockEntity machine;
    @Nullable
    private final BlockPos hostPos;
    private final int[] data = new int[DATA_COUNT];

    public CompressionEditorMenu(int containerId, Inventory playerInventory,
                                 @Nullable CompressionEditorBlockEntity machine) {
        this(containerId, playerInventory, machine, machine != null ? machine.getExponent() : 0);
    }

    public CompressionEditorMenu(int containerId, Inventory playerInventory,
                                 @Nullable CompressionEditorBlockEntity machine, int initialExponent) {
        super(ModMenus.COMPRESSION_EDITOR.get(), containerId);
        this.machine = machine;
        this.hostPos = machine != null ? machine.getBlockPos() : null;
        this.data[DATA_EXPONENT] = initialExponent;

        ItemStackHandler slotHandler = machine != null
                ? machine.getSlot() : new ItemStackHandler(1);

        addSlot(new SlotItemHandler(slotHandler, 0, 80, 41) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CompressionEditorBlockEntity.isCobblestone(stack);
            }
        });

        addPlayerInventory(playerInventory);
        addDataSlots(this);
    }

    public static CompressionEditorMenu client(int containerId, Inventory playerInventory,
                                               RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int exponent = buf.readVarInt();
        CompressionEditorBlockEntity machine =
                playerInventory.player.level().getBlockEntity(pos) instanceof CompressionEditorBlockEntity be ? be : null;
        return new CompressionEditorMenu(containerId, playerInventory, machine, exponent);
    }

    public int exponent() {
        return data[DATA_EXPONENT];
    }

    public void setExponentFromClient(int exponent) {
        if (machine != null) {
            machine.setExponent(exponent);
        }
    }

    @Override
    public void broadcastChanges() {
        if (machine != null && machine.getLevel() != null && !machine.getLevel().isClientSide()) {
            data[DATA_EXPONENT] = machine.getExponent();
        }
        super.broadcastChanges();
    }

    @Override
    public int getCount() {
        return DATA_COUNT;
    }

    @Override
    public int get(int index) {
        return index >= 0 && index < DATA_COUNT ? data[index] : 0;
    }

    @Override
    public void set(int index, int value) {
        if (index >= 0 && index < DATA_COUNT) {
            data[index] = value;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index == MACHINE_INDEX) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (CompressionEditorBlockEntity.isCobblestone(stack)) {
            if (!moveItemStackTo(stack, MACHINE_INDEX, MACHINE_INDEX + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return hostPos != null && hostPos.closerToCenterThan(player.position(), 8.0D);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 120 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 178));
        }
    }
}
