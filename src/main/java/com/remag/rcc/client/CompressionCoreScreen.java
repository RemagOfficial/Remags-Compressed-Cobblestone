package com.remag.rcc.client;

import com.remag.rcc.RCC;
import com.remag.rcc.menu.CompressionCoreMenu;
import com.remag.rcc.network.BceGoalTierPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Renders the Compression Manipulator state: the seed slot plus three vertical gauges (fuel-mass ratio on the
 * left, craft progress and energy on the right of the machine slots). Each gauge is filled bottom-up from the
 * separate {@code compression_core_fill.png} strip (crop for the detailed fuel/energy art, stretch for the solid
 * green progress). All numbers come from the {@link CompressionCoreMenu}'s synced {@code ContainerData} mirror,
 * so this screen never touches the block entity directly. Hovering a bar shows its exact readout (fuel mass,
 * craft percentage, stored energy) via {@link #renderTooltip}, which also forwards to the super slot-item tooltip.
 */
public class CompressionCoreScreen extends AbstractContainerScreen<CompressionCoreMenu> {
    private static final int TITLE_COLOR = 0x404040;

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RCC.MODID, "textures/gui/container/compression_core.png");
    // The Auto Compressor and Auto Decompressor share this screen but each has its own sheet: identical gauge wells
    // and slot positions, plus a small notch between the input and output slots that marks where the goal-tier text
    // box sits. The compressor sheet is art-identical to the decompressor's; it is a separate file only for
    // organisation, so the same field geometry lines up on both.
    private static final ResourceLocation DECOMPRESSOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RCC.MODID, "textures/gui/container/decompressor.png");
    private static final ResourceLocation COMPRESSOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RCC.MODID, "textures/gui/container/compressor.png");
    // Separate fill strip (64x77): three bottom-up bar graphics side by side - cobblestone (fuel), solid green
    // (craft progress), red (energy). White separator lines sit at x=20/40/60; the coloured interior is y=4..72.
    private static final ResourceLocation FILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RCC.MODID, "textures/gui/container/compression_core_fill.png");
    private static final int FILL_TEX_W = 64;
    private static final int FILL_TEX_H = 77;
    private static final int FILL_V0 = 4;   // first row of the coloured interior
    private static final int FILL_IH = 69;  // interior height (rows 4..72)
    // Left edge of each gauge's source column within the fill strip.
    private static final int FUEL_U = 1;
    private static final int PROG_U = 21;
    private static final int POWER_U = 41;
    // On-screen gauge geometry: each bar's top-left, matched to the wells baked into the GUI texture. Nudge the
    // three X constants (and BAR_Y) together with the texture if the alignment is off.
    private static final int BAR_W = 19;
    private static final int BAR_H = FILL_IH; // 1:1 vertical for the cropped (fuel/energy) bars
    private static final int BAR_Y = 34;
    private static final int FUEL_X = 14;
    private static final int PROG_X = 102;
    private static final int POWER_X = 139;

    // Goal-tier input for the Auto Compressor / Auto Decompressor (null for the manipulator).
    @Nullable
    private EditBox goalField;

    public CompressionCoreScreen(CompressionCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 106;
    }

    @Override
    protected void init() {
        super.init();
        if (this.menu.hasGoalField()) {
            // The goal-tier number, typed and sent on Enter. It is a normal bordered field, centred on the notch
            // baked into the Auto machines' sheets (x56..81, y65..73) so the box reads as its alignment guide - the
            // field covers it, so the drawn box never shows. There is no separate text label (no room beside it).
            EditBox field = new EditBox(this.font, this.leftPos + 48, this.topPos + 63, 40, 12,
                    Component.translatable("container.rcc.compression_core.goal"));
            field.setMaxLength(16);
            field.setBordered(true);
            field.setValue(this.menu.getGoalTierString());
            field.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
            field.setHint(Component.literal("0").withStyle(ChatFormatting.GRAY).copy());
            this.goalField = this.addRenderableWidget(field);
        } else {
            this.goalField = null;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.goalField != null && this.goalField.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            this.sendGoalTier();
            this.goalField.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Push the typed goal tier to the server; blank input is ignored (the machine keeps its current goal). */
    private void sendGoalTier() {
        if (this.goalField == null) {
            return;
        }
        String value = this.goalField.getValue().trim();
        if (!value.isEmpty()) {
            PacketDistributor.sendToServer(new BceGoalTierPayload(value));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Panel + baked bar wells from the main sheet. Each Auto machine has its own sheet (both add the goal-tier box
        // notch); the manipulator uses the base sheet. The 9-arg blit carries the real 176x202 texture size so the art
        // is drawn 1:1 (the 7-arg overload would assume a 256x256 sheet and mis-scale it).
        ResourceLocation background = this.menu.isCompressor() ? COMPRESSOR_TEXTURE
                : this.menu.isDecompressor() ? DECOMPRESSOR_TEXTURE : TEXTURE;
        graphics.blit(background, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

        // Fuel gauge (cobblestone, cropped bottom-up by current/required mass in log10 space).
        blitBarCrop(graphics, x + FUEL_X, y + BAR_Y, FUEL_U, fuelFraction());

        // Craft-progress gauge (solid green, stretched bottom-up - no detail to preserve, so scaling is fine).
        blitBarStretch(graphics, x + PROG_X, y + BAR_Y, PROG_U, progressFraction());

        // Energy gauge (red, cropped bottom-up).
        blitBarCrop(graphics, x + POWER_X, y + BAR_Y, POWER_U, energyFraction());
    }

    // --- Gauge fractions (shared by the bar fill and the hover tooltips) -----------------------

    /** Fuel pool relative to the seed's requirement: {@code 10^(current-required)} capped to 1 (0 with no seed). */
    private double fuelFraction() {
        double current = this.menu.currentFuelLog();
        double required = this.menu.requiredFuelLog();
        if (required == Double.NEGATIVE_INFINITY || Double.isNaN(required)) {
            return 0.0;
        }
        return Math.min(1.0, Math.pow(10.0, Math.min(0.0, current - required)));
    }

    private double progressFraction() {
        return (double) this.menu.progress() / Math.max(1, this.menu.progressMax());
    }

    private double energyFraction() {
        return (double) this.menu.energy() / Math.max(1, this.menu.maxEnergy());
    }

    /**
     * Reveals the bottom {@code fraction} of a gauge column without distorting it (crop): the source slice and the
     * destination rectangle share the same height, so the cobblestone / energy art stays pixel-crisp. Uses the
     * 1:1 9-arg {@code blit}.
     */
    private void blitBarCrop(GuiGraphics graphics, int bx, int by, int sourceU, double fraction) {
        int fillH = barFillHeight(fraction);
        if (fillH <= 0) {
            return;
        }
        int srcV = FILL_V0 + (BAR_H - fillH); // bottom-aligned slice of the coloured interior
        graphics.blit(FILL_TEXTURE, bx, by + (BAR_H - fillH),
                (float) sourceU, (float) srcV, BAR_W, fillH, FILL_TEX_W, FILL_TEX_H);
    }

    /**
     * Squashes the whole gauge column into the bottom {@code fraction} of the bar (stretch). Used for the solid
     * green progress bar. Uses the 11-arg {@code blit} whose destination size is independent of the source size.
     */
    private void blitBarStretch(GuiGraphics graphics, int bx, int by, int sourceU, double fraction) {
        int fillH = barFillHeight(fraction);
        if (fillH <= 0) {
            return;
        }
        graphics.blit(FILL_TEXTURE, bx, by + (BAR_H - fillH), BAR_W, fillH,
                (float) sourceU, (float) FILL_V0, BAR_W, BAR_H, FILL_TEX_W, FILL_TEX_H);
    }

    /** Pixels of a full-height bar that {@code fraction} (clamped to 0..1) fills, rounded to whole pixels. */
    private static int barFillHeight(double fraction) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, fraction)) * BAR_H);
    }

    // --- Hover tooltips -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // AbstractContainerScreen.render() does NOT draw tooltips itself; concrete screens must invoke this hook
        // (vanilla AbstractFurnaceScreen.render does the same). Without this call the hovered slot's item tooltip
        // never renders, which is why the slots looked tooltip-less.
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY); // hovered-slot item tooltip, like every other menu
        if (hoverBar(mouseX, mouseY, FUEL_X)) {
            graphics.renderTooltip(this.font, fuelTooltip(), Optional.empty(), mouseX, mouseY);
        } else if (hoverBar(mouseX, mouseY, PROG_X)) {
            graphics.renderTooltip(this.font, progressTooltip(), Optional.empty(), mouseX, mouseY);
        } else if (hoverBar(mouseX, mouseY, POWER_X)) {
            graphics.renderTooltip(this.font, powerTooltip(), Optional.empty(), mouseX, mouseY);
        }
    }

    /** True when the cursor is inside the vertical bar column whose on-screen left edge is {@code barLeftX}. */
    private boolean hoverBar(int mouseX, int mouseY, int barLeftX) {
        int bx = this.leftPos + barLeftX;
        int by = this.topPos + BAR_Y;
        return mouseX >= bx && mouseX < bx + BAR_W && mouseY >= by && mouseY < by + BAR_H;
    }

    private List<Component> fuelTooltip() {
        return List.of(
                Component.literal("Base Cobblestone Equivalent").withStyle(ChatFormatting.GOLD),
                Component.literal(powerOfTen(this.menu.currentFuelLog()) + " / "
                        + powerOfTen(this.menu.requiredFuelLog())));
    }

    private List<Component> progressTooltip() {
        int percent = (int) Math.round(Math.clamp(progressFraction(), 0.0, 1.0) * 100.0);
        return List.of(
                Component.literal("Progress").withStyle(ChatFormatting.GOLD),
                Component.literal(percent + "%"));
    }

    private List<Component> powerTooltip() {
        return List.of(
                Component.literal("Energy").withStyle(ChatFormatting.GOLD),
                Component.literal(String.format(Locale.US, "%,d / %,d FE", this.menu.energy(), this.menu.maxEnergy())));
    }

    /** Render a log10 mass as a compact {@code 10^N} cobblestone string; an empty pool shows as {@code 0}. */
    private static String powerOfTen(double log) {
        if (log == Double.NEGATIVE_INFINITY || Double.isNaN(log) || log <= 0.0) {
            return "0";
        }
        if (Math.abs(log - Math.rint(log)) < 1e-3) {
            return "10^" + (long) Math.rint(log);
        }
        return String.format(Locale.US, "10^%.1f", log);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw the labels ourselves (instead of super) so the "not formed" warning can take over the gap above
        // the inventory without colliding with the "Inventory" label.
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY + 10, TITLE_COLOR, false);
        if (this.menu.formed()) {
            graphics.drawString(this.font, this.playerInventoryTitle,
                    this.inventoryLabelX, this.inventoryLabelY, TITLE_COLOR, false);
        }
        if (!this.menu.formed()) {
            Component warning = Component.translatable("container.rcc.compression_core.unformed");
            int bx = (this.imageWidth - this.font.width(warning)) / 2;
            graphics.drawString(this.font, warning, bx, this.inventoryLabelY, 0xFFFF8080, false);
        }
        // No goal-tier text label: the decompressor's bordered field sits over the notch baked into its own sheet,
        // and there is no room beside it (the fuel gauge is to the left) for a "Goal tier:" caption.
    }
}
