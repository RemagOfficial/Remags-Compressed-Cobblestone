package com.remag.rcc.datagen.loot;

import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.registry.CompressionBand;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModBlockLootTables extends BlockLootSubProvider {
    public ModBlockLootTables(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        this.add(ModBlocks.COMPRESSED_COBBLESTONE.get(),
                this.createSingleItemTable(ModBlocks.COMPRESSED_COBBLESTONE.get().asItem())
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)));

        List<Block> structural = new ArrayList<>(List.of(
                ModBlocks.FRAME.get(), ModBlocks.GLASS.get(), ModBlocks.INTERFACE.get(),
                ModBlocks.PORT.get(), ModBlocks.CORE.get(), ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get(),
                ModBlocks.COMPRESSION_CORE_2.get(), ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_2.get(),
                ModBlocks.COMPRESSION_AUTO.get(),
                ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR.get(), ModBlocks.BCE_STORAGE.get(),
                ModBlocks.BCE_STORAGE_2.get(), ModBlocks.BCE_STORAGE_3.get()));
        structural.addAll(blocksOf(ModBlocks.COMPRESSION_CORES_BY_BAND));
        structural.add(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_3.get());
        structural.addAll(blocksOf(ModBlocks.BCE_STORAGES_BY_BAND));
        structural.add(ModBlocks.CREATIVE_BCE_SOURCE.get());
        structural.add(ModBlocks.COMPRESSION_EDITOR.get());
        for (Block block : structural) {
            this.add(block, this.createSingleItemTable(block.asItem()));
        }

        this.add(ModBlocks.BCE_PIPE.get(),
                this.createSingleItemTable(ModBlocks.BCE_PIPE.get().asItem())
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)));

        this.add(ModBlocks.COBBLESTONE_GENERATOR.get(),
                this.createSingleItemTable(ModBlocks.COBBLESTONE_GENERATOR.get().asItem())
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)));
        this.add(ModBlocks.GENERATOR_CORE.get(),
                this.createSingleItemTable(ModBlocks.GENERATOR_CORE.get().asItem())
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)));
        this.add(ModBlocks.GENERATOR_CORE_2.get(),
                this.createSingleItemTable(ModBlocks.GENERATOR_CORE_2.get().asItem())
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)));
        for (Block core : blocksOf(ModBlocks.GENERATOR_CORES_BY_BAND)) {
            this.add(core, this.createSingleItemTable(core.asItem())
                    .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)));
        }
    }

    
    private static List<Block> blocksOf(Map<CompressionBand, ? extends DeferredBlock<? extends Block>> banded) {
        return banded.values().stream().map(v -> (Block) v.get()).toList();
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(entry -> (Block) entry.get()).toList();
    }
}
