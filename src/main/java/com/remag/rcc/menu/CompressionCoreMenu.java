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

/**
 * Backing container for the Compression Manipulator GUI, opened through a
 * {@link com.remag.rcc.machine.CompressionInterfaceBlock}. It exposes the single seed slot (and a display-only
 * output slot) plus a {@link ContainerData} mirror of the core's energy / progress / fuel-mass scalars so the
 * screen can render them without reading the server-side block entity directly.
 *
 * <p>The two fuel magnitudes are log10-base-cobblestone masses that can exceed {@code int}/{@code long}, so they
 * travel as the raw bits of a {@code float} packed into an {@code int} data slot — plenty of precision for a
 * "10^N" readout.
 */
public class CompressionCoreMenu extends AbstractContainerMenu implements ContainerData {
    // Slot-index layout: [0 seed, 1 output, 2.. player inventory].
    private static final int SEED_INDEX = 0;
    private static final int OUTPUT_INDEX = 1;
    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private static final int DATA_COUNT = 7;
    private static final int IDX_ENERGY = 0;
    private static final int IDX_MAX_ENERGY = 1;
    private static final int IDX_PROGRESS = 2;
    private static final int IDX_PROGRESS_MAX = 3;
    private static final int IDX_FORMED = 4;
    private static final int IDX_REQUIRED = 5;
    private static final int IDX_CURRENT = 6;

    @Nullable
    private final ICompressionMachine machine;
    @Nullable
    private final BlockPos hostPos;
    /** Client-side snapshot of the host machine's goal tier (compressor or decompressor) used to prefill the text
     *  field; empty for every other machine. */
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
        // Both machines are block entities, so the anchor position for stillValid comes from the BE cast; the
        // interface itself deliberately stays free of world/position concerns.
        this.hostPos = machine instanceof BlockEntity be ? be.getBlockPos() : null;

        ItemStackHandler seedHandler = machine != null ? machine.getSeed() : new ItemStackHandler(1);
        ItemStackHandler outputHandler = machine != null ? machine.getOutput() : new ItemStackHandler(1);

        // Machine slots (indexes 0 and 1) must come before the player inventory for quickMoveStack ranges.
        addSlot(new SlotItemHandler(seedHandler, 0, 61, 41) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine == null ? ICompressionMachine.isSeed(stack) : machine.acceptsSeed(stack);
            }
        });
        addSlot(new SlotItemHandler(outputHandler, 0, 61, 83) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // output is take-only; the machine fills it
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

    /** Client-side reconstruction from the {@code openMenu(provider, pos)} buffer. */
    public static CompressionCoreMenu client(int containerId, Inventory playerInventory,
                                             RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        // The Auto Compressor and Auto Decompressor append their goal tier to the open buffer; other machines write
        // only the position, so read the string only when extra bytes remain.
        String goal = buf.readableBytes() > 0 ? buf.readUtf(64) : "";
        ICompressionMachine machine =
                playerInventory.player.level().getBlockEntity(pos) instanceof ICompressionMachine m ? m : null;
        return new CompressionCoreMenu(containerId, playerInventory, machine, goal);
    }

    // --- Goal tier (Auto Compressor / Auto Decompressor) ---------------------------------------

    /** True when the host machine offers a selectable goal tier (either Auto machine). Drives the text field. */
    public boolean hasGoalField() {
        return machine instanceof IGoalTierTarget;
    }

    /** True when the host is the Auto Decompressor (selects its panel art). */
    public boolean isDecompressor() {
        return machine instanceof IDecompressTarget;
    }

    /** True when the host is the Auto Compressor (selects its panel art). */
    public boolean isCompressor() {
        return machine instanceof ICompressTarget;
    }

    /** The goal tier used to prefill the screen's text field (a client-side snapshot from the open buffer). */
    public String getGoalTierString() {
        return goalTierDisplay;
    }

    /** Apply a client-submitted goal tier string to the machine (server side only; ignores malformed / non-target). */
    public void setGoalTierFromClient(String raw) {
        if (!(machine instanceof IGoalTierTarget target) || raw == null || raw.isBlank()) {
            return;
        }
        try {
            target.setGoalTier(new BigInteger(raw.trim()));
        } catch (NumberFormatException ignored) {
            // Leave the previous goal untouched on malformed input.
        }
    }

    // --- Data mirror ---------------------------------------------------------------------------

    @Override
    public void broadcastChanges() {
        // Only the server instance reads real machine state; the client mirror is fed through setData below.
        if (machine instanceof BlockEntity be && be.getLevel() != null && !be.getLevel().isClientSide()) {
            data[IDX_ENERGY] = machine.getEnergy().getEnergyStored();
            data[IDX_MAX_ENERGY] = machine.getEnergy().getMaxEnergyStored();
            data[IDX_PROGRESS] = machine.getProgress();
            data[IDX_PROGRESS_MAX] = machine.getProgressMax();
            data[IDX_FORMED] = machine.isFormed() ? 1 : 0;
            data[IDX_REQUIRED] = Float.floatToRawIntBits((float) machine.getRequiredFuel());
            data[IDX_CURRENT] = Float.floatToRawIntBits((float) machine.getFuelPool());
        }
        super.broadcastChanges();
    }

    // Screen-facing accessors (read the synced mirror, valid on both sides).

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

    /** Current fuel mass as a log10-base-cobblestone exponent ({@link Double#NEGATIVE_INFINITY} when empty). */
    public double currentFuelLog() {
        return Float.intBitsToFloat(data[IDX_CURRENT]);
    }

    /** Required fuel mass (log10 exponent) for the seed currently in the slot. */
    public double requiredFuelLog() {
        return Float.intBitsToFloat(data[IDX_REQUIRED]);
    }

    // --- ContainerData -------------------------------------------------------------------------

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

    // --- Interaction -------------------------------------------------------------------------

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
            // Machine slots (seed/output; guides are always empty) -> player inventory.
            if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (machine != null ? machine.acceptsSeed(stack) : ICompressionMachine.isSeed(stack)) {
            // Player inventory -> seed slot.
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
        // Close if the player moves more than ~8 blocks from the core.
        return player.distanceToSqr(hostPos.getX() + 0.5, hostPos.getY() + 0.5, hostPos.getZ() + 0.5) <= 64.0;
    }
}
