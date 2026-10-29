package com.remag.rcc.client.mixin;

import com.remag.rcc.client.MachineItemBadge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stamps a machine's tier (top-left) and level (top-right) onto its item icon in every container screen
 * (creative menu, inventory, machine GUIs). Injected at the tail of {@code renderSlot}, which draws in
 * GUI-relative coordinates (the slot loop pre-translates by the screen origin), so badges use raw slot.x/y
 * like vanilla's item draws. Deliberately @Shadow-free: only public API, nothing fragile to reobf.
 */
@Mixin(AbstractContainerScreen.class)
public class MachineItemBadgeMixin {

    private static final int TIER_COLOR = 0xFFFFFF;
    private static final int LEVEL_COLOR = 0xFFFFA0;
    // Half-size pose scale: a digit is 6px wide at full scale, 3px here, so a two-digit level and the tier
    // digit both fit inside the 16px icon without colliding.
    private static final float BADGE_SCALE = 0.5F;

    @Inject(method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V",
            at = @At("TAIL"))
    private void rcc$renderBadge(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        MachineItemBadge.Badge badge = MachineItemBadge.of(slot.getItem());
        if (badge == null) {
            return;
        }
        int x = slot.x;
        int y = slot.y;

        // 3D block-item icons reach ~z=450 inside the slot layer while text adds z=200; a +300 lift puts the
        // badges in front of any item model yet still under tooltips and highlights.
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        graphics.pose().scale(BADGE_SCALE, BADGE_SCALE, BADGE_SCALE);

        Font font = Minecraft.getInstance().font;
        String tier = Integer.toString(badge.tier());
        graphics.drawString(font, tier, (float) x / BADGE_SCALE, (float) y / BADGE_SCALE, TIER_COLOR, true);

        Integer level = badge.level();
        if (level != null) {
            String text = Integer.toString(level);
            float levelX = ((float) (x + 16) / BADGE_SCALE) - font.width(text);
            graphics.drawString(font, text, levelX, (float) y / BADGE_SCALE, LEVEL_COLOR, true);
        }

        graphics.pose().popPose();
    }
}
