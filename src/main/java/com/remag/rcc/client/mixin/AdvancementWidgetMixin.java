package com.remag.rcc.client.mixin;

import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Raises the advancement tooltip's hard title-width cap so the cobblestone-generator progression line can keep its
 * full Latin milestone names.
 *
 * <p>Vanilla {@link AdvancementWidget} builds each tooltip title with {@code font.substrByWidth(display.getTitle(), 163)}
 * - a {@code private static final int TITLE_MAX_WIDTH} that javac inlines into the constructor as the literal {@code 163}
 * - and it truncates with no ellipsis. The generator advancements are titled after the compressed-cobblestone milestone
 * they ask for (e.g. {@code "Quinquagintaquadringentuple Compressed Cobblestone"}), and that first unbreakable Latin word
 * overruns 163px, so the tail gets chopped. Bumping the cap to a value above the widest name we ship lets the title render
 * whole; the box then sizes itself to the actual title width, exactly as any long vanilla name would.
 *
 * <p>This is the mod's one deliberate exception to its "ships no mixins" stance: the effect is purely cosmetic, targets a
 * single integer constant, and - because a constructor name and an int literal are both unmapped - needs no refmap, so it
 * is behaviourally identical in dev and in the released (Mojang-mapped, non-obfuscated) jar.
 */
@Mixin(AdvancementWidget.class)
public class AdvancementWidgetMixin {
    /** The vanilla cap ({@code TITLE_MAX_WIDTH}) javac inlines into the constructor's {@code substrByWidth} call. */
    private static final int VANILLA_TITLE_MAX_WIDTH = 163;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = VANILLA_TITLE_MAX_WIDTH))
    private int widenTitleCap(int constant) {
        // Comfortably above the widest generator milestone name (~240px); titles shorter than this are unaffected.
        return 512;
    }
}
