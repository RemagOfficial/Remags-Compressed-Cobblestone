package com.remag.rcc.machine;

import net.minecraft.util.StringRepresentable;

/**
 * What a {@link CompressionPortBlock} does. One block covers all three kinds; the {@code PORT_TYPE}
 * block state selects the behaviour, and the block entity exposes the matching NeoForge capability.
 *
 * <ul>
 *   <li>{@link #POWER} — receives FE into the multiblock's core energy buffer.</li>
 *   <li>{@link #ITEM_IN} — swallows fed compressed cobblestone into the core's fuel-mass pool.</li>
 *   <li>{@link #ITEM_OUT} — exposes the core's finished-product slot to automation.</li>
 * </ul>
 */
public enum PortType implements StringRepresentable {
    POWER("power"),
    ITEM_IN("item_in"),
    ITEM_OUT("item_out");

    private final String name;

    PortType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
