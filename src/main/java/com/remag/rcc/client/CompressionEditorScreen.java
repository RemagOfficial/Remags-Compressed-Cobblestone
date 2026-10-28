package com.remag.rcc.client;

import com.remag.rcc.RCC;
import com.remag.rcc.machine.CompressionEditorBlockEntity;
import com.remag.rcc.menu.CompressionEditorMenu;
import com.remag.rcc.network.CompressionEditorTierPayload;
import com.remag.rcc.registry.BceNotation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;

public class CompressionEditorScreen extends AbstractContainerScreen<CompressionEditorMenu> {
    private static final int TITLE_COLOR = 0x404040;

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RCC.MODID, "textures/gui/container/compression_editor.png");

    private static final int SLIDER_X = 28;
    private static final int SLIDER_Y = 80;
    private static final int SLIDER_W = 120;
    private static final int SLIDER_H = 16;
    private static final int HANDLE_W = 8;
    private static final int HANDLE_FACE = 0xFFC6C6C6;
    private static final int HANDLE_FACE_HOVER = 0xFFE0E0E0;
    private static final int HANDLE_LIGHT = 0xFFFFFFFF;
    private static final int HANDLE_SHADOW = 0xFF555555;
    private static final int READOUT_Y = 65;
    private static final int TITLE_Y = 16;

    @Nullable
    private TierSlider slider;

    public CompressionEditorScreen(CompressionEditorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 106;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.slider = this.addRenderableWidget(new TierSlider(
                this.leftPos + SLIDER_X, this.topPos + SLIDER_Y, SLIDER_W, SLIDER_H,
                CompressionEditorBlockEntity.MAX_EXPONENT, this.menu.exponent(),
                value -> PacketDistributor.sendToServer(new CompressionEditorTierPayload(value))));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.slider != null && button == 0 && this.slider.isFocused()) {
            return this.slider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, TITLE_Y, TITLE_COLOR, false);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, TITLE_COLOR, false);
        Component readout = tierLabel(this.slider != null ? this.slider.getValue() : this.menu.exponent());
        graphics.drawString(this.font, readout, (this.imageWidth - this.font.width(readout)) / 2, READOUT_Y,
                TITLE_COLOR, false);
    }

    private static Component tierLabel(int exponent) {
        return Component.translatable("container.rcc.compression_editor.tier",
                "10" + BceNotation.superscript(Integer.toString(exponent)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private static final class TierSlider extends AbstractWidget {
        private final int maxExponent;
        private final IntConsumer onChange;
        private int value;

        TierSlider(int x, int y, int width, int height, int maxExponent, int initialValue, IntConsumer onChange) {
            super(x, y, width, height, Component.translatable("container.rcc.compression_editor.slider"));
            this.maxExponent = Math.max(1, maxExponent);
            this.onChange = onChange;
            this.value = clamp(initialValue);
        }

        int getValue() {
            return value;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, tierLabel(this.value));
        }

        private int clamp(int candidate) {
            return Math.max(0, Math.min(this.maxExponent, candidate));
        }

        private int travel() {
            return Math.max(1, this.width - HANDLE_W);
        }

        private int handleX() {
            return this.getX() + (int) Math.round((double) travel() * this.value / this.maxExponent);
        }

        private void applyFromCursor(double mouseX) {
            double ratio = (mouseX - this.getX() - HANDLE_W / 2.0D) / travel();
            this.updateValue((int) Math.round(ratio * this.maxExponent));
        }

        private void updateValue(int candidate) {
            int clamped = clamp(candidate);
            if (clamped != this.value) {
                this.value = clamped;
                this.onChange.accept(clamped);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.applyFromCursor(mouseX);
        }

        @Override
        public void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            this.applyFromCursor(mouseX);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (scrollY == 0.0D) {
                return false;
            }
            this.updateValue(this.value + (scrollY > 0.0D ? 1 : -1));
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            this.setFocused(false);
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int hx = handleX();
            int top = this.getY();
            int bottom = top + this.getHeight();
            int right = hx + HANDLE_W;
            graphics.fill(hx, top, right, bottom,
                    this.isMouseOver(mouseX, mouseY) ? HANDLE_FACE_HOVER : HANDLE_FACE);
            graphics.fill(hx, top, right, top + 1, HANDLE_LIGHT);
            graphics.fill(hx, top, hx + 1, bottom, HANDLE_LIGHT);
            graphics.fill(right - 1, top + 1, right, bottom, HANDLE_SHADOW);
            graphics.fill(hx + 1, bottom - 1, right, bottom, HANDLE_SHADOW);
        }
    }
}
