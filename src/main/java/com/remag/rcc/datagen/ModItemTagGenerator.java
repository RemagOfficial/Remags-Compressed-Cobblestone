package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.registry.ModItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModItemTagGenerator extends ItemTagsProvider {
    public ModItemTagGenerator(PackOutput p_275343_, CompletableFuture<HolderLookup.Provider> p_275729_,
                               CompletableFuture<TagLookup<Block>> p_275322_, @Nullable ExistingFileHelper existingFileHelper) {
        super(p_275343_, p_275729_, p_275322_, RCC.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        // Publish the wrench onto the shared convention tag, which is the whole interop story: our tool then configures
        // other mods' machines, and any wrench already in the pack (Mekanism's configurator, Create's wrench, ...) is
        // accepted by BcePipeBlock's useItemOn through the same tag. Writing to another namespace's tag is normal -
        // tags merge across packs at runtime.
        tag(ModItemTags.WRENCH).add(ModItems.WRENCH.get());
    }
}
