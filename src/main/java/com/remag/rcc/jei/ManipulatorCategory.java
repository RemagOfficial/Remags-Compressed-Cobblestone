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

/**
 * JEI category for the Compression Manipulator's doubling cycle. Because the machine is block-entity driven and
 * tier-agnostic (it accepts any seed tier at runtime), JEI can't derive a concrete recipe; the plugin supplies a
 * couple of pinned {@link ManipulatorDisplayRecipe} examples (seed {@code n} -> tier {@code 2n}).
 *
 * <p>The FE cost and the (astronomically large) fuel mass are drawn as plain text under the slots rather than as
 * custom energy/fluid drawables. The energy figure comes straight off the recipe and the fuel is the per-seed
 * value computed by the plugin from {@link com.remag.rcc.registry.CompressionConfig}, keeping the numbers honest.
 */
public class ManipulatorCategory implements IRecipeCategory<ManipulatorDisplayRecipe> {

    public static final RecipeType<ManipulatorDisplayRecipe> TYPE =
            RecipeType.create(RCC.MODID, "compression_manipulator", ManipulatorDisplayRecipe.class);

    private static final int SEED_X = 10;
    private static final int SLOT_Y = 8;
    private static final int OUTPUT_X = 84;
    private static final int TEXT_X = 2;
    private static final int TEXT_Y = 30;
    private static final int LINE_H = 10;
    private static final int TEXT_COLOR = 0xFFE0E0E0;

    private final IDrawable icon;

    public ManipulatorCategory(IGuiHelper guiHelper) {
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

    @Override
    public int getWidth() {
        return 118;
    }

    @Override
    public int getHeight() {
        return 62;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ManipulatorDisplayRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(SEED_X, SLOT_Y).addItemStack(recipe.seed());
        builder.addOutputSlot(OUTPUT_X, SLOT_Y).addItemStack(recipe.output());
    }

    @Override
    public void draw(ManipulatorDisplayRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
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
