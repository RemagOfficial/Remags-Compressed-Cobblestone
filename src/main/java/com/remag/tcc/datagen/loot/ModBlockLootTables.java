package com.remag.tcc.datagen.loot;

import com.remag.tcc.block.ModBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class ModBlockLootTables extends BlockLootSubProvider {
    public ModBlockLootTables() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        this.dropSelf(ModBlocks.COBBLESTONE_ONE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_TWO.get());
        this.dropSelf(ModBlocks.COBBLESTONE_THREE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FOUR.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FIVE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_SIX.get());
        this.dropSelf(ModBlocks.COBBLESTONE_SEVEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_EIGHT.get());
        this.dropSelf(ModBlocks.COBBLESTONE_NINE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_TEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_ELEVEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_TWELVE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_THIRTEEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FOURTEEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FIFTEEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_SIXTEEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_SEVENTEEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_EIGHTEEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_NINETEEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_TWENTY.get());
        this.dropSelf(ModBlocks.COBBLESTONE_TWENTYONE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_TWENTYTWO.get());
        this.dropSelf(ModBlocks.COBBLESTONE_TWENTYTHREE.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
