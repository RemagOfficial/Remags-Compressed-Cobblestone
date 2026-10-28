package com.remag.rcc.client.mixin;

import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AdvancementWidget.class)
public class AdvancementWidgetMixin {
    private static final int VANILLA_TITLE_MAX_WIDTH = 163;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = VANILLA_TITLE_MAX_WIDTH))
    private int widenTitleCap(int constant) {
        return 512;
    }
}
