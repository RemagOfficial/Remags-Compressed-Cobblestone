package com.remag.rcc.datagen.loot;

import com.remag.rcc.block.ModBlocks;
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
        this.dropSelf(ModBlocks.COBBLESTONE_TWENTYFOUR.get());
        this.dropSelf(ModBlocks.COBBLESTONE_TWENTYFIVE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_TWENTYSIX.get());
        this.dropSelf(ModBlocks.COBBLESTONE_TWENTYSEVEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_TWENTYEIGHT.get());
        this.dropSelf(ModBlocks.COBBLESTONE_TWENTYNINE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_THIRTY.get());
        this.dropSelf(ModBlocks.COBBLESTONE_THIRTYONE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_THIRTYTWO.get());
        this.dropSelf(ModBlocks.COBBLESTONE_THIRTYTHREE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_THIRTYFOUR.get());
        this.dropSelf(ModBlocks.COBBLESTONE_THIRTYFIVE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_THIRTYSIX.get());
        this.dropSelf(ModBlocks.COBBLESTONE_THIRTYSEVEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_THIRTYEIGHT.get());
        this.dropSelf(ModBlocks.COBBLESTONE_THIRTYNINE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FORTY.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FORTYONE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FORTYTWO.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FORTYTHREE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FORTYFOUR.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FORTYFIVE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FORTYSIX.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FORTYSEVEN.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FORTYEIGHT.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FORTYNINE.get());
        this.dropSelf(ModBlocks.COBBLESTONE_FIFTY.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
