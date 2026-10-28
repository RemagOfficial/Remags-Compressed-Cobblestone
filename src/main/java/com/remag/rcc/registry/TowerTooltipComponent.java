package com.remag.rcc.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record TowerTooltipComponent(Component leading, BceNotation.Tower tower, Component trailing)
        implements TooltipComponent {

    public static TowerTooltipComponent of(Component leading, BceNotation.Tower tower) {
        return new TowerTooltipComponent(leading, tower, null);
    }
}
