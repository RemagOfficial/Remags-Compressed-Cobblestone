package com.remag.rcc.menu;

import com.remag.rcc.machine.ICompressionMachine;
import com.remag.rcc.machine.ICompressTarget;
import com.remag.rcc.machine.IDecompressTarget;
import com.remag.rcc.machine.IGoalTierTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public class CompressionCoreMenu extends AbstractContainerMenu implements ContainerData {
    private static final int SEED_INDEX = 0;
    private static final int OUTPUT_INDEX = 1;
    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private static final int DATA_COUNT = 10;
    private static final int IDX_ENERGY = 0;
    private static final int IDX_MAX_ENERGY = 1;
    private static final int IDX_PROGRESS = 2;
    private static final int IDX_PROGRESS_MAX = 3;
    private static final int IDX_FORMED = 4;
    private static final int IDX_REQUIRED = 5;
    private static final int IDX_REQUIRED_HI = 6;
    private static final int IDX_CURRENT = 7;
    private static final int IDX_CURRENT_HI = 8;
    private static final int IDX_STATUS = 9;

    @Nullable
    private final ICompressionMachine machine;
    @Nullable
    private final BlockPos hostPos;
    private final String goalTierDisplay;
    private final int[] data = new int[DATA_COUNT];

    public CompressionCoreMenu(int containerId, Inventory playerInventory, @Nullable ICompressionMachine machine) {
        this(containerId, playerInventory, machine,
                machine instanceof IGoalTierTarget d ? d.goalTierString() : "");
    }

    public CompressionCoreMenu(int containerId, Inventory playerInventory, @Nullable ICompressionMachine machine,
                               String goalTierDisplay) {
        super(ModMenus.CORE.get(), containerId);
        this.machine = machine;
        this.goalTierDisplay = goalTierDisplay == null ? "" : goalTierDisplay;
        this.hostPos = machine instanceof BlockEntity be ? be.getBlockPos() : null;

        ItemStackHandler seedHandler = machine != null ? machine.getSeed() : new ItemStackHandler(1);
        ItemStackHandler outputHandler = machine != null ? machine.getOutput() : new ItemStackHandler(1);

        addSlot(new SlotItemHandler(seedHandler, 0, 61, 41) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine == null ? ICompressionMachine.isSeed(stack) : machine.acceptsSeed(stack);
            }
        });
        addSlot(new SlotItemHandler(outputHandler, 0, 61, 83) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventory(playerInventory);
        addDataSlots(this);
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

    public static CompressionCoreMenu client(int containerId, Inventory playerInventory,
                                             RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String goal = buf.readableBytes() > 0 ? buf.readUtf(64) : "";
        ICompressionMachine machine =
                playerInventory.player.level().getBlockEntity(pos) instanceof ICompressionMachine m ? m : null;
        return new CompressionCoreMenu(containerId, playerInventory, machine, goal);
    }

    public boolean hasGoalField() {
        return machine instanceof IGoalTierTarget;
    }

    public boolean isDecompressor() {
        return machine instanceof IDecompressTarget;
    }

    public boolean isCompressor() {
        return machine instanceof ICompressTarget;
    }

    public String getGoalTierString() {
        return goalTierDisplay;
    }

    public void setGoalTierFromClient(String raw) {
        if (!(machine instanceof IGoalTierTarget target) || raw == null || raw.isBlank()) {
            return;
        }
        try {
            target.setGoalTier(new BigInteger(raw.trim()));
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void broadcastChanges() {
        if (machine instanceof BlockEntity be && be.getLevel() != null && !be.getLevel().isClientSide()) {
            data[IDX_ENERGY] = machine.getEnergy().getEnergyStored();
            data[IDX_MAX_ENERGY] = machine.getEnergy().getMaxEnergyStored();
            data[IDX_PROGRESS] = machine.getProgress();
            data[IDX_PROGRESS_MAX] = machine.getProgressMax();
            data[IDX_FORMED] = machine.isFormed() ? 1 : 0;
            packDouble(IDX_REQUIRED, machine.getRequiredFuel());
            packDouble(IDX_CURRENT, machine.getFuelPool());
            data[IDX_STATUS] = machine.craftStatus().ordinal();
        }
        super.broadcastChanges();
    }

    private void packDouble(int loIndex, double value) {
        long bits = Double.doubleToRawLongBits(value);
        data[loIndex] = (int) bits;
        data[loIndex + 1] = (int) (bits >> 32);
    }

    private double unpackDouble(int loIndex) {
        long bits = (data[loIndex] & 0xFFFFFFFFL) | ((long) data[loIndex + 1] << 32);
        return Double.longBitsToDouble(bits);
    }

    public int energy() {
        return data[IDX_ENERGY];
    }

    public int maxEnergy() {
        return data[IDX_MAX_ENERGY];
    }

    public int progress() {
        return data[IDX_PROGRESS];
    }

    public int progressMax() {
        return data[IDX_PROGRESS_MAX];
    }

    public boolean formed() {
        return data[IDX_FORMED] != 0;
    }

    public ICompressionMachine.CraftStatus status() {
        ICompressionMachine.CraftStatus[] values = ICompressionMachine.CraftStatus.values();
        int ordinal = data[IDX_STATUS];
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ICompressionMachine.CraftStatus.READY;
    }

    public double currentFuelLog() {
        return unpackDouble(IDX_CURRENT);
    }

    public double requiredFuelLog() {
        return unpackDouble(IDX_REQUIRED);
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

        if (index < PLAYER_INV_START) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (machine != null ? machine.acceptsSeed(stack) : ICompressionMachine.isSeed(stack)) {
            if (!moveItemStackTo(stack, SEED_INDEX, SEED_INDEX + 1, false)) {
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
        if (hostPos == null) {
            return false;
        }
        double reach = 8.0 + (machine != null ? machine.guiShellRadius() : 0) * Math.sqrt(3.0);
        return player.distanceToSqr(hostPos.getX() + 0.5, hostPos.getY() + 0.5, hostPos.getZ() + 0.5) <= reach * reach;
    }
}
