package com.remag.rcc.network;

import com.remag.rcc.RCC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CompressionEditorTierPayload(int exponent) implements CustomPacketPayload {
    public static final Type<CompressionEditorTierPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RCC.MODID, "compression_editor_tier"));

    public static final StreamCodec<ByteBuf, CompressionEditorTierPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CompressionEditorTierPayload::exponent,
            CompressionEditorTierPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
