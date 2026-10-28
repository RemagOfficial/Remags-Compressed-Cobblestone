package com.remag.rcc.event;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.registry.ModCapabilities;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class CommonEvents {

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COMPRESSION_PORT.get(),
                (port, side) -> port.getEnergyCapability(side));

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COMPRESSION_PORT.get(),
                (port, side) -> port.getItemCapability(side));

        event.registerBlockEntity(
                ModCapabilities.BCE,
                ModBlockEntities.COMPRESSION_PORT.get(),
                (port, side) -> port.getBceCapability(side));

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COMPRESSION_AUTO.get(),
                (machine, side) -> machine.getEnergy());

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COMPRESSION_AUTO.get(),
                (machine, side) -> machine.getItemHandler(side));

        event.registerBlockEntity(
                ModCapabilities.BCE,
                ModBlockEntities.COMPRESSION_AUTO.get(),
                (machine, side) -> machine.getBceFuel(side));

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

        event.registerBlockEntity(
                ModCapabilities.BCE,
                ModBlockEntities.BCE_STORAGE.get(),
                (tank, side) -> tank.getBceStorage(side));

        event.registerBlockEntity(
                ModCapabilities.BCE,
                ModBlockEntities.BCE_PIPE.get(),
                (pipe, side) -> null);

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
