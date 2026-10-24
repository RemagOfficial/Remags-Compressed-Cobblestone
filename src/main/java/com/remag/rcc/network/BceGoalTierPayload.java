package com.remag.rcc.network;

import com.remag.rcc.RCC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -&gt; server message that sets the Auto Decompressor's goal tier to the typed decimal string. Sent when the
 * player confirms the GUI text field; the server parses and clamps it
 * (see {@link com.remag.rcc.menu.CompressionCoreMenu#setGoalTierFromClient(String)}).
 */
public record BceGoalTierPayload(String goal) implements CustomPacketPayload {
    public static final Type<BceGoalTierPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RCC.MODID, "bce_goal_tier"));

    public static final StreamCodec<ByteBuf, BceGoalTierPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BceGoalTierPayload::goal,
            BceGoalTierPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
