package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.ModBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagGenerator extends BlockTagsProvider {

    public ModBlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, RCC.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.COMPRESSED_COBBLESTONE.get())
                .add(ModBlocks.FRAME.get())
                .add(ModBlocks.PORT.get())
                .add(ModBlocks.CORE.get())
                .add(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get())
                .add(ModBlocks.INTERFACE.get())
                .add(ModBlocks.GLASS.get())
                .add(ModBlocks.COMPRESSION_AUTO.get())
                .add(ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR.get())
                .add(ModBlocks.BCE_PIPE.get())
                .add(ModBlocks.BCE_STORAGE.get())
                .add(ModBlocks.BCE_STORAGE_2.get())
                .add(ModBlocks.BCE_STORAGE_3.get())
                .add(ModBlocks.COBBLESTONE_GENERATOR.get())
                .add(ModBlocks.GENERATOR_CORE.get())
                .add(ModBlocks.COMPRESSION_CORE_2.get())
                .add(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_2.get())
                .add(ModBlocks.GENERATOR_CORE_2.get())
                .add(bandedBlocks(ModBlocks.COMPRESSION_CORES_BY_BAND))
                .add(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_3.get())
                .add(bandedBlocks(ModBlocks.GENERATOR_CORES_BY_BAND))
                .add(bandedBlocks(ModBlocks.BCE_STORAGES_BY_BAND))
                .add(ModBlocks.CREATIVE_BCE_SOURCE.get())
                .add(ModBlocks.COMPRESSION_EDITOR.get());

        this.tag(ModBlockTags.BCE_CONNECTABLE)
                .add(ModBlocks.BCE_STORAGE.get())
                .add(ModBlocks.BCE_STORAGE_2.get())
                .add(ModBlocks.BCE_STORAGE_3.get())
                .add(bandedBlocks(ModBlocks.BCE_STORAGES_BY_BAND))
                .add(ModBlocks.CREATIVE_BCE_SOURCE.get())
                .add(ModBlocks.COMPRESSION_AUTO.get())
                .add(ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR.get())
                .add(ModBlocks.PORT.get())
                .add(ModBlocks.COBBLESTONE_GENERATOR.get());
    }

    
    private static Block[] bandedBlocks(java.util.Map<CompressionBand, ? extends net.neoforged.neoforge.registries.DeferredBlock<? extends Block>> banded) {
        return banded.values().stream().map(java.util.function.Supplier::get).toArray(Block[]::new);
    }
}
