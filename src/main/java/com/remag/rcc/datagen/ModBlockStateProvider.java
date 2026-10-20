package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, RCC.MODID, exFileHelper);

    }

    @Override
    protected void registerStatesAndModels() {
        // generate block states for all blocks
        ModBlocks.BLOCKS.getEntries().forEach(this::blockWithItem);
    }

    private void blockWithItem(DeferredHolder<Block,? extends Block> blockDeferredHolder) {
        simpleBlockWithItem(blockDeferredHolder.get(), cubeAll(blockDeferredHolder.get()));
    }
}
