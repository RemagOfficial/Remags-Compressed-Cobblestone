package com.remag.rcc.client;

import com.remag.rcc.registry.BceNotation;
import com.remag.rcc.registry.TowerTooltipComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/**
 * The client half of {@link TowerTooltipComponent}: draws a real power tower, one level raised above the next.
 *
 * <p>Layout is a single left-to-right pen. The label sits on the tooltip's line, then each tower level is drawn smaller
 * and higher than the one below it - {@link #SCALE_STEP} of its size, {@link #RAISE_PX} pixels up - so
 * {@code 9^(10^12)} arrives as a stack of three growing-downward glyphs instead of the {@code 9^(10¹²)} bracket-work a
 * string has to fall back on. The unit (if any) drops back to the base line after the tower, which is where a rate's
 * {@code "BCE/sec"} belongs.
 *
 * <p>Drawing goes through {@link GuiGraphics#drawString} inside a pushed pose per level, so the scaling is just a model
 * transform of the tooltip's own matrix - no font provider, no texture, no mixin. {@link #getWidth} and
 * {@link #getHeight} walk the <em>same</em> pen the drawing does, which is not cosmetic: vanilla measures every
 * component to size the background box, then lays the components out top to bottom using those same heights, so a
 * mismatch spills text outside the frame. The whole tower is shifted down by its own height above the baseline so the
 * raised levels stay inside the slot reserved for them instead of clipping into the line above.
 *
 * <p>The drawing happens in {@link #renderImage} rather than {@link #renderText} because of the order vanilla uses
 * ({@code GuiGraphics.renderTooltipInternal}): it calls {@code renderText} on every component, then restarts at the top
 * and calls {@code renderImage} on every one at the same running y - so the image pass is the layer that composites
 * over item art, and both passes land on the same line either way.
 */
public final class ClientTowerTooltip implements ClientTooltipComponent {
    /**
     * Each level up is drawn this fraction of the size of the level below it. Sized so the third level of the deepest
     * tower a shipped tier can produce ({@code 10^(9.54 x 10^99)}) is still about four pixels tall - any smaller and
     * the top of the stack stops being legible at the GUI's own scale.
     */
    private static final float SCALE_STEP = 0.7F;
    /** Pixels a level is raised by, times the size of the level it sits on (so a deep tower stays inside its slot). */
    private static final float RAISE_PX = 4.0F;
    /** Gap, in pixels, between the label / unit and the tower - the drawn equivalent of a space. */
    private static final int GAP_PX = 2;
    /** Tooltip gray, so the drawn line matches the text lines it replaces. */
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

    /**
     * How far the topmost level reaches above the base line: the sum of every step's raise, measured in the size of
     * the level it stands on. The drawing shifts down by exactly this much.
     */
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
