package com.remag.tcc.block;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeModeTab {
    public static final CreativeModeTab TCC_TAB = new CreativeModeTab("tcctab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModBlocks.COBBLESTONE_ONE.get().asItem());
        }
    };
}
