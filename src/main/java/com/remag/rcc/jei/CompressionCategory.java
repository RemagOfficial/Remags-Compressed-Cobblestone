package com.remag.rcc.jei;

import com.remag.rcc.RCC;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.registry.ModDataComponents;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.List;

/**
 * A single JEI category that shows both halves of the compression loop: a full 3x3 grid of one tier crafting
 * the next tier up, and a single item breaking back down to nine of the tier below. Because every real recipe
 * is dynamic, the plugin feeds this category a handful of concrete {@link CompressionDisplayRecipe} examples.
 *
 * <p>The ingredient/output stacks carry the {@code rcc:compression_level} component, so they render through the
 * same runtime {@link com.remag.rcc.event.ClientEvents item tint} as everything else — each example tier shows its
 * real shade.
 */
public class CompressionCategory implements IRecipeCategory<CompressionDisplayRecipe> {

    public static final RecipeType<CompressionDisplayRecipe> TYPE =
            RecipeType.create(RCC.MODID, "compression", CompressionDisplayRecipe.class);

    private static final int SPACING = 18;
    private static final int GRID_X = 2;
    private static final int GRID_Y = 2;
    private static final int OUTPUT_X = GRID_X + 3 * SPACING + 24;
    private static final int OUTPUT_Y = GRID_Y + SPACING;

    private final IDrawable icon;

    public CompressionCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(levelStack(BigInteger.valueOf(5)));
    }

    @Override
    public RecipeType<CompressionDisplayRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Compressed Cobblestone");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    // JEI's default getWidth()/getHeight() dereference getBackground(), which is null for a background-less
    // category, so both must be overridden or category registration throws and the whole plugin is skipped.
    @Override
    public int getWidth() {
        return OUTPUT_X + SPACING + 2;
    }

    @Override
    public int getHeight() {
        return GRID_Y + 3 * SPACING + 2;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CompressionDisplayRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> inputs = recipe.inputs();
        if (inputs.size() >= 9) {
            // Compression: a full 3x3 of the same tier.
            for (int i = 0; i < 9; i++) {
                int x = GRID_X + (i % 3) * SPACING;
                int y = GRID_Y + (i / 3) * SPACING;
                builder.addInputSlot(x, y).addItemStack(inputs.get(i));
            }
        } else if (!inputs.isEmpty()) {
            // Decompression: a single item, shown centred in the grid area.
            builder.addInputSlot(GRID_X + SPACING, GRID_Y + SPACING).addItemStack(inputs.get(0));
        }
        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y).addItemStack(recipe.output());
    }

    private static ItemStack levelStack(BigInteger tier) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), tier);
        return stack;
    }
}
