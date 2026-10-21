package com.remag.rcc.datagen.loot;

import com.remag.rcc.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;

import java.util.Set;

public class ModBlockLootTables extends BlockLootSubProvider {
    public ModBlockLootTables(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        // Drop the block item, copying the block entity's compression_level component onto it so a
        // broken block returns the exact tier it was (read from BlockEntity#collectComponents).
        this.add(ModBlocks.COMPRESSED_COBBLESTONE.get(),
                this.createSingleItemTable(ModBlocks.COMPRESSED_COBBLESTONE.get().asItem())
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)));

        // Structure blocks drop themselves; no component copying (their tiers/state live on the core BE,
        // which is not part of these stacks).
        for (Block structural : new Block[]{
                ModBlocks.FRAME.get(), ModBlocks.GLASS.get(), ModBlocks.INTERFACE.get(),
                ModBlocks.PORT.get(), ModBlocks.CORE.get()}) {
            this.add(structural, this.createSingleItemTable(structural.asItem()));
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(entry -> (Block) entry.get()).toList();
    }
}
