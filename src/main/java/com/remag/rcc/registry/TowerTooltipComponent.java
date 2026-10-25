package com.remag.rcc.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * The common half of a <em>drawn</em> power tower: the label, the tower, and an optional unit - kept as data so a
 * server-safe item class can hand it to the tooltip without touching a single client type.
 *
 * <p>Minecraft can only put one custom graphic on a tooltip ( {@link net.minecraft.world.item.Item#getTooltipImage}
 * returns a single {@link TooltipComponent}), and it draws it after every text line, so this carries the whole
 * readout rather than just the exponent. The client half
 * ({@link com.remag.rcc.client.ClientTowerTooltip}) renders it with each level smaller and higher than the one below,
 * which is the one thing plain text cannot do: Unicode has superscript digits but no superscript of a superscript, so
 * a band-2 figure like {@code 9^(10^12)} can only ever be <em>bracketed</em> in a string. It is drawn here instead.
 *
 * @param leading the label in front of the number, e.g. {@code "BCE:"}; may be {@code null}
 * @param tower   the value, as produced by {@link BceNotation}
 * @param trailing the unit behind the number, e.g. {@code "BCE/sec"}; may be {@code null}
 */
public record TowerTooltipComponent(Component leading, BceNotation.Tower tower, Component trailing)
        implements TooltipComponent {

    /** A tower with a label in front of it and nothing behind - the compressed-cobblestone BCE readout. */
    public static TowerTooltipComponent of(Component leading, BceNotation.Tower tower) {
        return new TowerTooltipComponent(leading, tower, null);
    }
}
