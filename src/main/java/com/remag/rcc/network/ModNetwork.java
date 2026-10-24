package com.remag.rcc.network;

import com.remag.rcc.menu.CompressionCoreMenu;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the mod's custom network payloads. Wired from the mod constructor via
 * {@code modEventBus.addListener(ModNetwork::register)}.
 */
public final class ModNetwork {

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(BceGoalTierPayload.TYPE, BceGoalTierPayload.STREAM_CODEC, ModNetwork::handleGoalTier);
    }

    /** Runs on the server thread; apply the typed goal tier to the sender's open menu (if it is a decompressor). */
    private static void handleGoalTier(BceGoalTierPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof CompressionCoreMenu menu) {
            menu.setGoalTierFromClient(payload.goal());
        }
    }

    private ModNetwork() {
    }
}
