package com.remag.rcc.client;

import com.remag.rcc.RCC;
import com.remag.rcc.machine.ICompressionMachine.CraftStatus;
import com.remag.rcc.menu.CompressionCoreMenu;
import com.remag.rcc.network.BceGoalTierPayload;
import com.remag.rcc.registry.BceNotation;
import com.remag.rcc.registry.TowerListTooltipComponent;
import com.remag.rcc.registry.TowerTooltipComponent;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CompressionCoreScreen extends AbstractContainerScreen<CompressionCoreMenu> {
    private static final int TITLE_COLOR = 0x404040;

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RCC.MODID, "textures/gui/container/compression_core.png");
    private static final ResourceLocation DECOMPRESSOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RCC.MODID, "textures/gui/container/decompressor.png");
    private static final ResourceLocation COMPRESSOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RCC.MODID, "textures/gui/container/compressor.png");
    private static final ResourceLocation FILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RCC.MODID, "textures/gui/container/compression_core_fill.png");
    private static final int FILL_TEX_W = 64;
    private static final int FILL_TEX_H = 77;
    private static final int FILL_V0 = 4;
    private static final int FILL_IH = 69;
    private static final int FUEL_U = 1;
    private static final int PROG_U = 21;
    private static final int POWER_U = 41;
    private static final int BAR_W = 19;
    private static final int BAR_H = FILL_IH;
    private static final int BAR_Y = 34;
    private static final int FUEL_X = 14;
    private static final int PROG_X = 102;
    private static final int POWER_X = 139;

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

        ResourceLocation background = this.menu.isCompressor() ? COMPRESSOR_TEXTURE
                : this.menu.isDecompressor() ? DECOMPRESSOR_TEXTURE : TEXTURE;
        graphics.blit(background, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

        blitBarCrop(graphics, x + FUEL_X, y + BAR_Y, FUEL_U, fuelFraction());

        blitBarStretch(graphics, x + PROG_X, y + BAR_Y, PROG_U, progressFraction());

        blitBarCrop(graphics, x + POWER_X, y + BAR_Y, POWER_U, energyFraction());
    }

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

    private void blitBarCrop(GuiGraphics graphics, int bx, int by, int sourceU, double fraction) {
        int fillH = barFillHeight(fraction);
        if (fillH <= 0) {
            return;
        }
        int srcV = FILL_V0 + (BAR_H - fillH);
        graphics.blit(FILL_TEXTURE, bx, by + (BAR_H - fillH),
                (float) sourceU, (float) srcV, BAR_W, fillH, FILL_TEX_W, FILL_TEX_H);
    }

    private void blitBarStretch(GuiGraphics graphics, int bx, int by, int sourceU, double fraction) {
        int fillH = barFillHeight(fraction);
        if (fillH <= 0) {
            return;
        }
        graphics.blit(FILL_TEXTURE, bx, by + (BAR_H - fillH), BAR_W, fillH,
                (float) sourceU, (float) FILL_V0, BAR_W, BAR_H, FILL_TEX_W, FILL_TEX_H);
    }

    private static int barFillHeight(double fraction) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, fraction)) * BAR_H);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        if (hoverBar(mouseX, mouseY, FUEL_X)) {
            renderFuelTooltip(graphics, mouseX, mouseY);
        } else if (hoverBar(mouseX, mouseY, PROG_X)) {
            graphics.renderTooltip(this.font, progressTooltip(), Optional.empty(), mouseX, mouseY);
        } else if (hoverBar(mouseX, mouseY, POWER_X)) {
            graphics.renderTooltip(this.font, powerTooltip(), Optional.empty(), mouseX, mouseY);
        }
    }

    private boolean hoverBar(int mouseX, int mouseY, int barLeftX) {
        int bx = this.leftPos + barLeftX;
        int by = this.topPos + BAR_Y;
        return mouseX >= bx && mouseX < bx + BAR_W && mouseY >= by && mouseY < by + BAR_H;
    }

    private void renderFuelTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Component title = Component.literal("Base Cobblestone Equivalent").withStyle(ChatFormatting.GOLD);
        List<TowerTooltipComponent> rows = new ArrayList<>(2);
        rows.add(new TowerTooltipComponent(Component.literal("Stored: "),
                BceNotation.magnitude(this.menu.currentFuelLog()), null));
        rows.add(new TowerTooltipComponent(Component.literal("Required: "),
                BceNotation.magnitude(this.menu.requiredFuelLog()), null));
        graphics.renderTooltip(this.font, List.of(title),
                Optional.of(new TowerListTooltipComponent(rows)), mouseX, mouseY);
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

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY + 10, TITLE_COLOR, false);
        if (this.menu.formed()) {
            graphics.drawString(this.font, this.playerInventoryTitle,
                    this.inventoryLabelX, this.inventoryLabelY, TITLE_COLOR, false);
        }
        CraftStatus status = this.menu.status();
        if (status != CraftStatus.READY && status != CraftStatus.NO_SEED
                && status != CraftStatus.SEED_TOO_LOW && status != CraftStatus.SEED_AT_CAP) {
            Component reason = statusLine(status);
            int bx = (this.imageWidth - this.font.width(reason)) / 2;
            int by = this.menu.formed() ? this.inventoryLabelY - 11 : this.inventoryLabelY;
            graphics.drawString(this.font, reason, bx, by, statusColor(status), false);
        }
    }

    private static Component statusLine(CraftStatus status) {
        return switch (status) {
            case NOT_FORMED -> Component.translatable("container.rcc.compression_core.unformed");
            case NO_SEED, OUTPUT_FULL, SEED_AT_CAP, SEED_TOO_LOW, NEED_FUEL, NEED_ENERGY, READY -> Component.empty();
        };
    }

    private static int statusColor(CraftStatus status) {
        return status == CraftStatus.NOT_FORMED ? 0xFFFF8080 : 0xFFFFFF55;
    }
}
