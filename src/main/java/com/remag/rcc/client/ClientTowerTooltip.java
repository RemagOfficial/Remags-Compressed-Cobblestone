package com.remag.rcc.client;

import com.remag.rcc.registry.BceNotation;
import com.remag.rcc.registry.TowerTooltipComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public final class ClientTowerTooltip implements ClientTooltipComponent {
    private static final float SCALE_STEP = 0.7F;
    private static final float RAISE_PX = 4.0F;
    private static final int GAP_PX = 2;
    private static final int COLOR = ChatFormatting.GRAY.getColor() | 0xFF000000;

    private final TowerTooltipComponent data;

    public ClientTowerTooltip(TowerTooltipComponent data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return Minecraft.getInstance().font.lineHeight + (int) Math.ceil(topOffset());
    }

    @Override
    public int getWidth(Font font) {
        float pen = 0.0F;
        if (data.leading() != null) {
            pen += font.width(data.leading().getString()) + GAP_PX;
        }
        float scale = 1.0F;
        for (BceNotation.Tower level = data.tower(); level != null; level = level.exponent()) {
            pen += font.width(level.prefix()) * scale;
            scale *= SCALE_STEP;
        }
        if (data.trailing() != null) {
            pen += GAP_PX + font.width(data.trailing().getString());
        }
        return (int) Math.ceil(pen);
    }

    private float topOffset() {
        float offset = 0.0F;
        float scale = 1.0F;
        BceNotation.Tower level = data.tower();
        while (level != null && level.exponent() != null) {
            offset += RAISE_PX * scale;
            scale *= SCALE_STEP;
            level = level.exponent();
        }
        return offset;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        float pen = x;
        if (data.leading() != null) {
            String label = data.leading().getString();
            graphics.drawString(font, label, pen, y, COLOR, false);
            pen += font.width(label) + GAP_PX;
        }
        float base = y + topOffset();
        float scale = 1.0F;
        for (BceNotation.Tower level = data.tower(); level != null; level = level.exponent()) {
            String text = level.prefix();
            graphics.pose().pushPose();
            graphics.pose().translate(pen, base, 0.0F);
            graphics.pose().scale(scale, scale, scale);
            graphics.drawString(font, text, 0.0F, 0.0F, COLOR, false);
            graphics.pose().popPose();
            pen += font.width(text) * scale;
            if (level.exponent() != null) {
                base -= RAISE_PX * scale;
                scale *= SCALE_STEP;
            }
        }
        if (data.trailing() != null) {
            String unit = data.trailing().getString();
            graphics.drawString(font, unit, pen + GAP_PX, y, COLOR, false);
        }
    }
}
