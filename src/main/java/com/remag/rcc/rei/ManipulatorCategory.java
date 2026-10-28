package com.remag.rcc.rei;

import com.remag.rcc.RCC;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.registry.DisplayNames;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ManipulatorCategory implements DisplayCategory<ManipulatorDisplay> {

    public static final CategoryIdentifier<ManipulatorDisplay> IDENTIFIER =
            CategoryIdentifier.of(RCC.MODID, "compression_manipulator");

    private static final int TEXT_COLOR = 0xFF404040;
    private static final int TEXT_X = 12;

    private final Renderer icon = ReiItems.entry(new ItemStack(ModItems.COMPRESSION_CORE.get()));

    @Override
    public CategoryIdentifier<ManipulatorDisplay> getCategoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.rcc.manipulator.title");
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

    @Override
    public List<Widget> setupDisplay(ManipulatorDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        int slotY = bounds.y + 6;
        int inX = bounds.x + 14;
        int outX = bounds.x + 84;
        widgets.add(Widgets.createSlot(new Point(inX, slotY)).markInput()
                .entries(display.getRecipeInputs().get(0)));
        widgets.add(Widgets.createArrow(new Point(inX + 24, slotY)));
        widgets.add(Widgets.createSlot(new Point(outX, slotY)).markOutput()
                .entries(display.getOutputEntries().get(0)));

        int tx = bounds.x + TEXT_X;
        int ty = slotY + 24;
        for (Component line : infoLines(display)) {
            widgets.add(text(tx, ty, line));
            ty += 10;
        }
        return widgets;
    }

    private static List<Component> infoLines(ManipulatorDisplay display) {
        return List.of(
                Component.translatable("jei.rcc.manipulator.info.doubling"),
                Component.translatable("jei.rcc.manipulator.info.energy", display.energyPerCraft()),
                Component.translatable("jei.rcc.manipulator.info.fuel_value",
                        DisplayNames.bceMagnitude(display.requiredFuelLog())));
    }

    private static Widget text(int x, int y, Component message) {
        return Widgets.createLabel(new Point(x, y), message).color(TEXT_COLOR).noShadow().leftAligned();
    }
}
