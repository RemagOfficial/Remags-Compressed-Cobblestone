package com.remag.rcc.client;

import com.remag.rcc.registry.TowerListTooltipComponent;
import com.remag.rcc.registry.TowerTooltipComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.ArrayList;
import java.util.List;

public final class ClientTowerListTooltip implements ClientTooltipComponent {
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
