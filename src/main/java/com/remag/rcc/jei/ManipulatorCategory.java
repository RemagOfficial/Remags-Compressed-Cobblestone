package com.remag.rcc.jei;

import com.remag.rcc.RCC;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.registry.DisplayNames;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ManipulatorCategory implements IRecipeCategory<ManipulatorDisplayRecipe> {

    public static final RecipeType<ManipulatorDisplayRecipe> TYPE =
            RecipeType.create(RCC.MODID, "compression_manipulator", ManipulatorDisplayRecipe.class);

    private static final int SEED_X = 10;
    private static final int SLOT_Y = 8;
    private static final int OUTPUT_X = 84;
    private static final int ARROW_X = 45;
    private static final int ARROW_Y = SLOT_Y + 2;
    private static final int TEXT_X = 2;
    private static final int TEXT_Y = 30;
    private static final int LINE_H = 10;
    // Dark grey: JEI's recipe panel is light, so near-white text would be unreadable on it.
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int WIDTH = 150;
    private static final int HEIGHT = 62;

    private final IGuiHelper guiHelper;
    private final IDrawable icon;

    public ManipulatorCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.COMPRESSION_CORE.get()));
    }

    @Override
    public RecipeType<ManipulatorDisplayRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.rcc.manipulator.title");
    }

    // No getBackground() is supplied, so getWidth()/getHeight() must be overridden: JEI's defaults dereference a
    // null background and throw during category registration.
    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ManipulatorDisplayRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(SEED_X, SLOT_Y).addItemStack(recipe.seed()).setStandardSlotBackground();
        builder.addOutputSlot(OUTPUT_X, SLOT_Y).addItemStack(recipe.output()).setOutputSlotBackground();
    }

    @Override
    public void draw(ManipulatorDisplayRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        guiHelper.getRecipeArrow().draw(guiGraphics, ARROW_X, ARROW_Y);
        var font = Minecraft.getInstance().font;
        int y = TEXT_Y;
        guiGraphics.drawString(font, Component.translatable("jei.rcc.manipulator.info.doubling"), TEXT_X, y, TEXT_COLOR, false);
        y += LINE_H;
        guiGraphics.drawString(font, Component.translatable("jei.rcc.manipulator.info.energy", recipe.energyPerCraft()), TEXT_X, y, TEXT_COLOR, false);
        y += LINE_H;
        guiGraphics.drawString(font, Component.translatable("jei.rcc.manipulator.info.fuel_value",
                DisplayNames.bceMagnitude(recipe.requiredFuelLog())), TEXT_X, y, TEXT_COLOR, false);
    }
}
