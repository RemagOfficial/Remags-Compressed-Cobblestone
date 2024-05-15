package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagGenerator extends BlockTagsProvider {

    public ModBlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, RCC.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        // add mineable with pickaxe tag to all blocks using a loop
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.COBBLESTONE_ONE.get(),
                ModBlocks.COBBLESTONE_TWO.get(),
                ModBlocks.COBBLESTONE_THREE.get(),
                ModBlocks.COBBLESTONE_FOUR.get(),
                ModBlocks.COBBLESTONE_FIVE.get(),
                ModBlocks.COBBLESTONE_SIX.get(),
                ModBlocks.COBBLESTONE_SEVEN.get(),
                ModBlocks.COBBLESTONE_EIGHT.get(),
                ModBlocks.COBBLESTONE_NINE.get(),
                ModBlocks.COBBLESTONE_TEN.get(),
                ModBlocks.COBBLESTONE_ELEVEN.get(),
                ModBlocks.COBBLESTONE_TWELVE.get(),
                ModBlocks.COBBLESTONE_THIRTEEN.get(),
                ModBlocks.COBBLESTONE_FOURTEEN.get(),
                ModBlocks.COBBLESTONE_FIFTEEN.get(),
                ModBlocks.COBBLESTONE_SIXTEEN.get(),
                ModBlocks.COBBLESTONE_SEVENTEEN.get(),
                ModBlocks.COBBLESTONE_EIGHTEEN.get(),
                ModBlocks.COBBLESTONE_NINETEEN.get(),
                ModBlocks.COBBLESTONE_TWENTY.get(),
                ModBlocks.COBBLESTONE_TWENTYONE.get(),
                ModBlocks.COBBLESTONE_TWENTYTWO.get(),
                ModBlocks.COBBLESTONE_TWENTYTHREE.get(),
                ModBlocks.COBBLESTONE_TWENTYFOUR.get(),
                ModBlocks.COBBLESTONE_TWENTYFIVE.get(),
                ModBlocks.COBBLESTONE_TWENTYSIX.get(),
                ModBlocks.COBBLESTONE_TWENTYSEVEN.get(),
                ModBlocks.COBBLESTONE_TWENTYEIGHT.get(),
                ModBlocks.COBBLESTONE_TWENTYNINE.get(),
                ModBlocks.COBBLESTONE_THIRTY.get(),
                ModBlocks.COBBLESTONE_THIRTYONE.get(),
                ModBlocks.COBBLESTONE_THIRTYTWO.get(),
                ModBlocks.COBBLESTONE_THIRTYTHREE.get(),
                ModBlocks.COBBLESTONE_THIRTYFOUR.get(),
                ModBlocks.COBBLESTONE_THIRTYFIVE.get(),
                ModBlocks.COBBLESTONE_THIRTYSIX.get(),
                ModBlocks.COBBLESTONE_THIRTYSEVEN.get(),
                ModBlocks.COBBLESTONE_THIRTYEIGHT.get(),
                ModBlocks.COBBLESTONE_THIRTYNINE.get(),
                ModBlocks.COBBLESTONE_FORTY.get(),
                ModBlocks.COBBLESTONE_FORTYONE.get(),
                ModBlocks.COBBLESTONE_FORTYTWO.get(),
                ModBlocks.COBBLESTONE_FORTYTHREE.get(),
                ModBlocks.COBBLESTONE_FORTYFOUR.get(),
                ModBlocks.COBBLESTONE_FORTYFIVE.get(),
                ModBlocks.COBBLESTONE_FORTYSIX.get(),
                ModBlocks.COBBLESTONE_FORTYSEVEN.get(),
                ModBlocks.COBBLESTONE_FORTYEIGHT.get(),
                ModBlocks.COBBLESTONE_FORTYNINE.get(),
                ModBlocks.COBBLESTONE_FIFTY.get());
    }
}
