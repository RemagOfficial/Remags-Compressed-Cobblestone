package com.remag.rcc.event;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.registry.ModCapabilities;
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

        // Ports also carry BCE where their controller exposes it: an insert-mode pipe end burns BCE as fuel into the
        // Compression Manipulator's ITEM_IN fuel pool, while the Decompression Manipulator's item ports offer its BCE
        // buffer so the released mass can be piped out (or fed back in). The port delegates to the resolved controller,
        // returning null for a kind that carries none (so a POWER port, or the compressor's ITEM_OUT, stays inert).
        event.registerBlockEntity(
                ModCapabilities.BCE,
                ModBlockEntities.COMPRESSION_PORT.get(),
                (port, side) -> port.getBceCapability(side));

        // The single-block Auto-Compressor exposes everything on every face (no port blocks): a receive-only
        // energy buffer, and a combined item handler that takes fuel on insert and yields the finished output on
        // extract.
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COMPRESSION_AUTO.get(),
                (machine, side) -> machine.getEnergy());

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COMPRESSION_AUTO.get(),
                (machine, side) -> machine.getItemHandler(side));

        // The Auto-Compressor also burns BCE: an insert-mode pipe end pushes its bus's flow into the same log10 fuel
        // pool that cobblestone items feed, through a sink-only view (fuel already spent can never be piped back out).
        event.registerBlockEntity(
                ModCapabilities.BCE,
                ModBlockEntities.COMPRESSION_AUTO.get(),
                (machine, side) -> machine.getBceFuel(side));

        // The single-block Auto Decompressor: a receive-only energy buffer, a seed-in / output-out item handler, and
        // the new BCE capability so its released mass can (eventually) be piped out to BCE-powered machines.
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COMPRESSION_AUTO_DECOMPRESSOR.get(),
                (machine, side) -> machine.getEnergy());

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COMPRESSION_AUTO_DECOMPRESSOR.get(),
                (machine, side) -> machine.getItemHandler(side));

        event.registerBlockEntity(
                ModCapabilities.BCE,
                ModBlockEntities.COMPRESSION_AUTO_DECOMPRESSOR.get(),
                (machine, side) -> machine.getBceStorage(side));

        // --- BCE logistics -----------------------------------------------------------------
        // The standalone tank is the other BCE store in the mod: one log10 pool, offered on every face so any pipe
        // end (or any other mod) reaching it sees the same buffer.
        event.registerBlockEntity(
                ModCapabilities.BCE,
                ModBlockEntities.BCE_STORAGE.get(),
                (tank, side) -> tank.getBceStorage(side));

        // Pipes deliberately expose *nothing*: a pipe that answered the BCE query would be discovered as its own
        // network's source or sink (every segment touches another segment), and the bus would pump BCE into the
        // conduit between them. Registering an always-null handler states that intent where a reader of this file
        // would otherwise assume an oversight; it behaves identically to not registering at all.
        event.registerBlockEntity(
                ModCapabilities.BCE,
                ModBlockEntities.BCE_PIPE.get(),
                (pipe, side) -> null);

        // --- Cobblestone generators -----------------------------------------------------------
        // A generator answers whichever capability matches its rung: item rungs expose their cobblestone output on the
        // item handler (and null on BCE), BCE rungs expose their mass pool as a one-way source (and null on items).
        // Returning null for the mismatched side is what keeps, say, the base drip from ever appearing as a BCE store.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COBBLESTONE_GENERATOR.get(),
                (gen, side) -> gen.getItemHandler(side));

        event.registerBlockEntity(
                ModCapabilities.BCE,
                ModBlockEntities.COBBLESTONE_GENERATOR.get(),
                (gen, side) -> gen.getBceStorage(side));
    }

    private CommonEvents() {
    }
}
