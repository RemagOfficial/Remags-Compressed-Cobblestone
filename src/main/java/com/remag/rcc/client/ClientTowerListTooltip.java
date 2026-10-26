package com.remag.rcc.client;

import com.remag.rcc.registry.TowerListTooltipComponent;
import com.remag.rcc.registry.TowerTooltipComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * The client half of {@link TowerListTooltipComponent}: stacks several drawn power towers top to bottom in the single
 * graphic slot a tooltip offers.
 *
 * <p>Every row is delegated to a real {@link ClientTowerTooltip} - the exact renderer the machine items use - so the
 * fuel gauge's "Stored / Required" towers are pixel-identical to the tower on a generator core's tooltip. The list
 * only adds the vertical layout: {@link #getWidth} returns the widest row, {@link #getHeight} the summed row heights
 * plus a gap, and {@link #renderImage} walks the same running {@code y} so the drawn rows land inside the box vanilla
 * measured. Left edges share the one {@code x} they are handed, so the labels line up as a column.
 */
public final class ClientTowerListTooltip implements ClientTooltipComponent {
    /** Blank rows between consecutive towers, so two stacked readouts never touch. */
    private static final int ROW_GAP = 2;

    private final List<ClientTowerTooltip> rows;

    public ClientTowerListTooltip(TowerListTooltipComponent data) {
        this.rows = new ArrayList<>(data.rows().size());
        for (TowerTooltipComponent row : data.rows()) {
            this.rows.add(new ClientTowerTooltip(row));
        }
    }

    @Override
    public int getHeight() {
        int height = 0;
        for (int i = 0; i < this.rows.size(); i++) {
            if (i > 0) {
                height += ROW_GAP;
            }
            height += this.rows.get(i).getHeight();
        }
        return height;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (ClientTowerTooltip row : this.rows) {
            width = Math.max(width, row.getWidth(font));
        }
        return width;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        int rowTop = y;
        for (ClientTowerTooltip row : this.rows) {
            row.renderImage(font, x, rowTop, graphics);
            rowTop += row.getHeight() + ROW_GAP;
        }
    }
}
