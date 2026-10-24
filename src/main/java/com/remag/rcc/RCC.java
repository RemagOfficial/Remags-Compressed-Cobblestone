package com.remag.rcc;

import com.mojang.logging.LogUtils;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.block.ModCreativeModeTab;
import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.event.ClientEvents;
import com.remag.rcc.event.CommonEvents;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.menu.ModMenus;
import com.remag.rcc.network.ModNetwork;
import com.remag.rcc.recipe.ModRecipeSerializers;
import com.remag.rcc.registry.ModDataComponents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
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

        ModDataComponents.REGISTRAR.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        ModCreativeModeTab.TABS.register(modEventBus);
        ModMenus.register(modEventBus);

        // Port block capabilities (energy / item handler) are needed on both sides.
        modEventBus.addListener(CommonEvents::registerCapabilities);

        // Custom network payloads (the Auto Decompressor's goal-tier setter) are registered on both sides.
        modEventBus.addListener(ModNetwork::register);

        // Runtime per-level tinting is client-only; guard so the class (and its client-only event types)
        // is never loaded on a dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEvents.register(modEventBus);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }
}
