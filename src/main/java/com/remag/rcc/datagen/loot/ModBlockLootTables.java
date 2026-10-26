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
                ModBlocks.PORT.get(), ModBlocks.CORE.get(), ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get(),
                ModBlocks.COMPRESSION_CORE_2.get(), ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_2.get(),
                ModBlocks.COMPRESSION_AUTO.get(),
                ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR.get(), ModBlocks.BCE_STORAGE.get(),
                ModBlocks.BCE_STORAGE_2.get(), ModBlocks.BCE_STORAGE_3.get()}) {
            this.add(structural, this.createSingleItemTable(structural.asItem()));
        }

        // A pipe must come back as the variant that was placed, not as the block's default tier-1 all-off state: the
        // block entity publishes its tier/mode as the block_state component, which this copies onto the drop.
        this.add(ModBlocks.BCE_PIPE.get(),
                this.createSingleItemTable(ModBlocks.BCE_PIPE.get().asItem())
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)));

        // Same for the generator: its rung lives only in the block state, and the block entity publishes it as the
        // block_state component, so a broken generator drops the tier it was rather than defaulting to BASE. The
        // tier-1 generator multiblock's core rides the identical route for its GeneratorCoreTier rung.
        this.add(ModBlocks.COBBLESTONE_GENERATOR.get(),
                this.createSingleItemTable(ModBlocks.COBBLESTONE_GENERATOR.get().asItem())
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)));
        this.add(ModBlocks.GENERATOR_CORE.get(),
                this.createSingleItemTable(ModBlocks.GENERATOR_CORE.get().asItem())
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)));
        // The band-2 generator core rides the same rung-carrying route as band 1 (its block supplies the band).
        this.add(ModBlocks.GENERATOR_CORE_2.get(),
                this.createSingleItemTable(ModBlocks.GENERATOR_CORE_2.get().asItem())
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(entry -> (Block) entry.get()).toList();
    }
}
