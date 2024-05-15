package com.remag.tcc.datagen;

import com.remag.tcc.TCC;
import com.remag.tcc.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, TCC.MODID, exFileHelper);

    }

    @Override
    protected void registerStatesAndModels() {
        // generate block states for all blocks
        ModBlocks.BLOCKS.getEntries().forEach(this::blockWithItem);
    }

    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }
}
