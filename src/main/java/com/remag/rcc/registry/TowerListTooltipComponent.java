package com.remag.rcc.registry;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

/**
 * A vertical run of drawn power towers, for the one place a single tooltip must show <em>more than one</em> BCE
 * figure as a tower - the Compression Manipulator's fuel gauge, which reads both the stored mass and the seed's
 * requirement side by side.
 *
 * <p>A vanilla tooltip carries exactly one custom graphic (the {@code Optional<TooltipComponent>} argument of
 * {@code GuiGraphics#renderTooltip}, or an item's single {@code getTooltipImage}), so two towers cannot be passed as
 * two images. This bundles them into the one allowed slot, and the client half
 * ({@link com.remag.rcc.client.ClientTowerListTooltip}) paints each row with the same
 * {@link com.remag.rcc.client.ClientTowerTooltip} the item tooltips use, stacked top to bottom. Each row keeps its own
 * leading label (e.g. {@code "Stored:"}) so the pair stays legible.
 *
 * <p>It reuses {@link TowerTooltipComponent} rows verbatim, so an item's single-tower readout and a machine's
 * multi-tower readout share one data model and one pixel layout - no second renderer to keep in sync.
 *
 * @param rows the towers to draw, in top-to-bottom order; never empty in the shipped use
 */
public record TowerListTooltipComponent(List<TowerTooltipComponent> rows) implements TooltipComponent {
}
