package com.remag.rcc.network;

import com.remag.rcc.menu.CompressionCoreMenu;
import com.remag.rcc.menu.CompressionEditorMenu;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(BceGoalTierPayload.TYPE, BceGoalTierPayload.STREAM_CODEC, ModNetwork::handleGoalTier);
        registrar.playToServer(CompressionEditorTierPayload.TYPE, CompressionEditorTierPayload.STREAM_CODEC,
                ModNetwork::handleEditorTier);
    }

    private static void handleEditorTier(CompressionEditorTierPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof CompressionEditorMenu menu) {
            menu.setExponentFromClient(payload.exponent());
        }
    }

    private static void handleGoalTier(BceGoalTierPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof CompressionCoreMenu menu) {
            menu.setGoalTierFromClient(payload.goal());
        }
    }

    private ModNetwork() {
    }
}
