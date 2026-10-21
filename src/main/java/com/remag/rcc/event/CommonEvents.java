package com.remag.rcc.event;

import com.remag.rcc.block.entity.ModBlockEntities;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Common (both-sides) registration hooks. Kept off the client-only {@link ClientEvents} so capabilities exist
 * on a dedicated server too.
 */
public final class CommonEvents {

    /**
     * Expose the Compression Manipulator ports as NeoForge block capabilities. The port block entity returns
     * the type-appropriate handler (and {@code null} for a kind the port does not provide), so a hopper/pipe
     * sees an item sink on ITEM_IN, an item source on ITEM_OUT, and an energy receiver on POWER.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COMPRESSION_PORT.get(),
                (port, side) -> port.getEnergyCapability(side));

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COMPRESSION_PORT.get(),
                (port, side) -> port.getItemCapability(side));
    }

    private CommonEvents() {
    }
}
