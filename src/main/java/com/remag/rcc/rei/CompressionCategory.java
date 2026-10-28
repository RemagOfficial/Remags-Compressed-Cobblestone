package com.remag.rcc.rei;

import com.remag.rcc.RCC;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.network.chat.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class CompressionCategory implements DisplayCategory<CompressionDisplay> {

    public static final CategoryIdentifier<CompressionDisplay> IDENTIFIER =
            CategoryIdentifier.of(RCC.MODID, "compression");

    private static final int SPACING = 18;

    private final Renderer icon = ReiItems.entry(ReiItems.levelStack(BigInteger.valueOf(5)));

    @Override
    public CategoryIdentifier<CompressionDisplay> getCategoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Compressed Cobblestone");
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

    @Override
    public List<Widget> setupDisplay(CompressionDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        List<EntryIngredient> inputs = display.getRecipeInputs();
        int gridX = bounds.x + 6;
        int gridY = bounds.y + (bounds.height - 3 * SPACING) / 2;
        if (inputs.size() >= 9) {
            for (int i = 0; i < 9; i++) {
                int x = gridX + (i % 3) * SPACING;
                int y = gridY + (i / 3) * SPACING;
                widgets.add(Widgets.createSlot(new Point(x, y)).markInput().entries(inputs.get(i)));
            }
        } else if (!inputs.isEmpty()) {
            widgets.add(Widgets.createSlot(new Point(gridX + SPACING, gridY + SPACING))
                    .markInput().entries(inputs.get(0)));
        }

        int outX = gridX + 3 * SPACING + 30;
        int outY = gridY + SPACING;
        widgets.add(Widgets.createArrow(new Point(gridX + 3 * SPACING + 2, outY)));
        if (!display.getOutputEntries().isEmpty()) {
            widgets.add(Widgets.createSlot(new Point(outX, outY)).markOutput()
                    .entries(display.getOutputEntries().get(0)));
        }
        return widgets;
    }
}
