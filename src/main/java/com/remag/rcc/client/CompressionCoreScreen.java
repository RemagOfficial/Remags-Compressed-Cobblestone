package com.remag.rcc.client;

import com.remag.rcc.RCC;
import com.remag.rcc.menu.CompressionCoreMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * Renders the Compression Manipulator state: the seed slot, a fuel readout (current vs required, shown as
 * power-of-ten because the masses span astronomically large ranges), an energy bar, and a craft-progress bar.
 * All numbers come from the {@link CompressionCoreMenu}'s synced {@code ContainerData} mirror, so this screen
 * never touches the block entity directly.
 */
public class CompressionCoreScreen extends AbstractContainerScreen<CompressionCoreMenu> {
    private static final int PANEL = 0xFF2B2B2B;
    private static final int TRACK = 0xFF404040;
    private static final int ENERGY = 0xFFFF5050;
    private static final int FUEL = 0xFF60E060;
    private static final int ARROW = 0xFFE0E0E0;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int TITLE_COLOR = 0x404040;

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RCC.MODID, "textures/gui/container/compression_core.png");

    public CompressionCoreScreen(CompressionCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 182;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 86;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Background texture (panel, slot wells and the empty gauges are baked into the art).
        // Use the overload that takes the real texture size: the 7-arg blit assumes a 256x256 sheet, which
        // would mis-scale our 176x182 art. imageWidth/imageHeight equal the PNG size, so UVs map 1:1.
        graphics.blit(TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

        // Vertical craft arrow between the two slots, filled top-down by progress.
        int progMax = Math.max(1, this.menu.progressMax());
        int arrowFill = clampRatio(this.menu.progress(), progMax, 36);
        graphics.fill(x + 96, y + 24, x + 104, y + 60, TRACK);
        graphics.fill(x + 96, y + 24, x + 104, y + 24 + arrowFill, ARROW);

        // Energy bar on the right, filled bottom-up.
        int energyMax = Math.max(1, this.menu.maxEnergy());
        int energyFill = clampRatio(this.menu.energy(), energyMax, 44);
        graphics.fill(x + 150, y + 20, x + 158, y + 64, TRACK);
        graphics.fill(x + 150, y + 64 - energyFill, x + 158, y + 64, ENERGY);

        // Fuel bar across the bottom of the upper area, filled by current/required mass ratio.
        double current = this.menu.currentFuelLog();
        double required = this.menu.requiredFuelLog();
        int fuelWidth = 74;
        graphics.fill(x + 12, y + 50, x + 12 + fuelWidth, y + 58, TRACK);
        if (required != Double.NEGATIVE_INFINITY && !Double.isNaN(required)) {
            double ratio = Math.pow(10.0, Math.min(0.0, current - required)); // 10^(cur-req) capped at 1
            int fill = (int) Math.round(Math.max(0.0, Math.min(1.0, ratio)) * fuelWidth);
            graphics.fill(x + 12, y + 50, x + 12 + fill, y + 58, FUEL);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw the labels ourselves (instead of super) so the "not formed" warning can take over the gap above
        // the inventory without colliding with the "Inventory" label.
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE_COLOR, false);
        if (this.menu.formed()) {
            graphics.drawString(this.font, this.playerInventoryTitle,
                    this.inventoryLabelX, this.inventoryLabelY, TITLE_COLOR, false);
        }
        graphics.drawString(this.font, fuelLine(), 12, 38, TEXT, false);
        if (!this.menu.formed()) {
            Component warning = Component.translatable("container.rcc.compression_core.unformed");
            int bx = (this.imageWidth - this.font.width(warning)) / 2;
            graphics.drawString(this.font, warning, bx, this.inventoryLabelY, 0xFFFF8080, false);
        }
    }

    private String fuelLine() {
        return "Fuel " + powerOfTen(this.menu.currentFuelLog()) + " / " + powerOfTen(this.menu.requiredFuelLog());
    }

    /** Render a log10 mass as a compact {@code 10^N} string; an empty pool shows as {@code 0}. */
    private static String powerOfTen(double log) {
        if (log == Double.NEGATIVE_INFINITY || Double.isNaN(log) || log <= 0.0) {
            return "0";
        }
        if (Math.abs(log - Math.rint(log)) < 1e-3) {
            return "10^" + (long) Math.rint(log);
        }
        return String.format(Locale.US, "10^%.1f", log);
    }

    private static int clampRatio(int value, int max, int span) {
        if (max <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(span, (int) Math.round((double) value / max * span)));
    }
}
