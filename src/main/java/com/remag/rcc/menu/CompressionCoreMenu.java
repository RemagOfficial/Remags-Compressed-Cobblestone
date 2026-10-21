package com.remag.rcc.menu;

import com.remag.rcc.machine.CompressionCoreBlockEntity;
import com.remag.rcc.registry.CompressionConfig;
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
    private final CompressionCoreBlockEntity core;
    @Nullable
    private final BlockPos hostPos;
    private final int[] data = new int[DATA_COUNT];

    public CompressionCoreMenu(int containerId, Inventory playerInventory,
                               @Nullable CompressionCoreBlockEntity core) {
        super(ModMenus.CORE.get(), containerId);
        this.core = core;
        this.hostPos = core != null ? core.getBlockPos() : null;

        ItemStackHandler seedHandler = core != null ? core.getSeed() : new ItemStackHandler(1);
        ItemStackHandler outputHandler = core != null ? core.getOutput() : new ItemStackHandler(1);

        // Machine slots (indexes 0 and 1) must come before the player inventory for quickMoveStack ranges.
        addSlot(new SlotItemHandler(seedHandler, 0, 61, 41) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CompressionCoreBlockEntity.isSeed(stack);
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
        CompressionCoreBlockEntity core =
                playerInventory.player.level().getBlockEntity(pos) instanceof CompressionCoreBlockEntity c ? c : null;
        return new CompressionCoreMenu(containerId, playerInventory, core);
    }

    // --- Data mirror ---------------------------------------------------------------------------

    @Override
    public void broadcastChanges() {
        if (core != null && core.getLevel() != null && !core.getLevel().isClientSide()) {
            data[IDX_ENERGY] = core.getEnergy().getEnergyStored();
            data[IDX_MAX_ENERGY] = CompressionConfig.ENERGY_CAPACITY;
            data[IDX_PROGRESS] = core.getProgress();
            data[IDX_PROGRESS_MAX] = CompressionConfig.TICKS_PER_CRAFT;
            data[IDX_FORMED] = core.isFormed() ? 1 : 0;
            data[IDX_REQUIRED] = Float.floatToRawIntBits((float) core.getRequiredFuel());
            data[IDX_CURRENT] = Float.floatToRawIntBits((float) core.getFuelPool());
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
        } else if (CompressionCoreBlockEntity.isSeed(stack)) {
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
