package com.remag.rcc;

import com.mojang.logging.LogUtils;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.block.ModCreativeModeTab;
import com.remag.rcc.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RCC.MODID)
public class RCC
{
    public static final String MODID = "rcc";
    private static final Logger LOGGER = LogUtils.getLogger();
    public RCC(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(this::commonSetup);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModCreativeModeTab.TABS.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }
}
