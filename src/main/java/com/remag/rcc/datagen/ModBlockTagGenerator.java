package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.registry.ModBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
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
        // The single compressed cobblestone block is pickaxe-minable.
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
                // BCE logistics: both are metal-family blocks, so both need a pickaxe (their drops require it too).
                .add(ModBlocks.BCE_PIPE.get())
                .add(ModBlocks.BCE_STORAGE.get())
                .add(ModBlocks.BCE_STORAGE_2.get())
                .add(ModBlocks.BCE_STORAGE_3.get())
                .add(ModBlocks.COBBLESTONE_GENERATOR.get())
                // The tier-1 generator multiblock's core: a metal machine block like the other cores. It is not
                // BCE_CONNECTABLE - it is sealed inside the tower, and pipes reach its pool through the ports.
                .add(ModBlocks.GENERATOR_CORE.get())
                // The band-2 cores: same metal family, sealed inside their shells, so pickaxe-only and not connectable.
                .add(ModBlocks.COMPRESSION_CORE_2.get())
                .add(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_2.get())
                .add(ModBlocks.GENERATOR_CORE_2.get());

        // Blocks a pipe may bolt an end to. This is the list the connection arms are drawn from, so it has to be
        // exactly the set of blocks that register the BCE capability (see CommonEvents) minus the pipe itself, which
        // joins pipes through its own rule. A block missing here is a store pipes will not touch; a block added here
        // without a capability is an arm that moves nothing.
        this.tag(ModBlockTags.BCE_CONNECTABLE)
                .add(ModBlocks.BCE_STORAGE.get())
                .add(ModBlocks.BCE_STORAGE_2.get())
                .add(ModBlocks.BCE_STORAGE_3.get())
                .add(ModBlocks.COMPRESSION_AUTO.get())
                .add(ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR.get())
                // Ports now register the BCE capability (delegating to whichever controller they resolve), so a pipe
                // end can bolt to one and burn BCE as fuel into the compressor / drain the decompressor's buffer.
                .add(ModBlocks.PORT.get())
                // The generator's BCE rungs register the capability (item rungs return null, so the arm simply moves
                // nothing on those - harmless and rare, only ever the base drip).
                .add(ModBlocks.COBBLESTONE_GENERATOR.get());
    }
}
