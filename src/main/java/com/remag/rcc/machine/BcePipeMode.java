package com.remag.rcc.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum BcePipeMode implements StringRepresentable {
    OFF("off"),
    EXTRACT("extract"),
    INSERT("insert"),
    PASSIVE("passive");

    private final String name;

    BcePipeMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public BcePipeMode toggle() {
        return switch (this) {
            case EXTRACT -> INSERT;
            case INSERT -> PASSIVE;
            case PASSIVE -> EXTRACT;
            case OFF -> EXTRACT;
        };
    }

    public boolean isLinked() {
        return this != OFF;
    }

    public Component displayName() {
        return Component.translatable("block.rcc.bce_pipe." + getSerializedName());
    }
}
