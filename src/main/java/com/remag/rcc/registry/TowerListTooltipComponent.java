package com.remag.rcc.registry;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

public record TowerListTooltipComponent(List<TowerTooltipComponent> rows) implements TooltipComponent {
}
