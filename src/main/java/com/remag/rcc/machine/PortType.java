package com.remag.rcc.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum PortType implements StringRepresentable {
    POWER("power"),
    ITEM_IN("item_in"),
    ITEM_OUT("item_out"),
    SEED("seed");

    private final String name;

    PortType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public Component displayName() {
        return Component.translatable("block.rcc.compression_port." + getSerializedName());
    }
}
