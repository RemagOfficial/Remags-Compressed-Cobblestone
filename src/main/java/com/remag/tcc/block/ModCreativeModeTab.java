package com.remag.tcc.block;

import com.remag.tcc.TCC;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModCreativeModeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TCC.MODID);

    public static final List<Supplier<? extends ItemLike>> TCC_TABS = new ArrayList<>();

    public static final RegistryObject<CreativeModeTab> TCC_TAB = TABS.register("tcc_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tcctab"))
                    .icon(ModBlocks.COBBLESTONE_ONE.get().asItem()::getDefaultInstance)
                    .displayItems((displayParams, output) ->
                            TCC_TABS.forEach(itemLike -> output.accept(new ItemStack(itemLike.get()))))
                    .build()
    );

    public static <T extends Item> RegistryObject<T> addToTab(RegistryObject<T> itemLike) {
        TCC_TABS.add(itemLike);
        return itemLike;
    }
}
