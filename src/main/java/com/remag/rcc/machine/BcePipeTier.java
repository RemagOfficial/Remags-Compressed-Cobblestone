package com.remag.rcc.machine;

import com.remag.rcc.registry.CompressionConfig;
import net.minecraft.util.StringRepresentable;

public enum BcePipeTier implements StringRepresentable {
    TIER_1("one"),
    TIER_2("two"),
    TIER_3("three"),
    TIER_4("four"),
    TIER_5("five"),
    TIER_6("six"),
    TIER_7("seven"),
    TIER_8("eight"),
    TIER_9("nine");

    private final String name;

    BcePipeTier(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public double maxTransferLog() {
        return CompressionConfig.BCE_PIPE_MAX_TRANSFER_LOG[this.ordinal()];
    }

    public int number() {
        return this.ordinal() + 1;
    }
}
