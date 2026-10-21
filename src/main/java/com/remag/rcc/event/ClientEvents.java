package com.remag.rcc.event;

import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.block.entity.CompressionBlockEntity;
import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.client.CompressionCoreRenderer;
import com.remag.rcc.client.CompressionCoreScreen;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.menu.ModMenus;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.math.BigInteger;

/**
 * Client-only runtime tinting. Because every tier is now one block/one item distinguished only by the
 * {@code rcc:compression_level} data component, the per-level colour that used to be baked into 10k models
 * is instead computed at render time from {@link CompressionConfig#tintArgb(java.math.BigInteger)}:
 *
 * <ul>
 *   <li>placed blocks read the level off the {@link CompressionBlockEntity};</li>
 *   <li>items (inventory, hand, dropped, and the block item) read the level off the stack.</li>
 * </ul>
 *
 * <p>The plain-cube block model has no baked colour, so this grayscale multiply is what darkens each tier.
 *
 * <p>Registered explicitly from the mod constructor behind a client-side guard so this class (which pulls
 * in client-only types) is never loaded on a dedicated server, and without relying on the deprecated
 * {@code @EventBusSubscriber(bus = ...)} attribute.
 */
public final class ClientEvents {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientEvents::onRegisterBlockColors);
        modEventBus.addListener(ClientEvents::onRegisterItemColors);
        modEventBus.addListener(ClientEvents::onRegisterMenuScreens);
        modEventBus.addListener(ClientEvents::onRegisterRenderers);
        modEventBus.addListener(ClientEvents::onClientSetup);
    }

    /** The core renders its (client-synced) seed as a large spinning 3D cube via this block entity renderer. */
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.COMPRESSION_CORE.get(), CompressionCoreRenderer::new);
    }

    /**
     * Blocks bake into the {@code solid} render layer by default, which ignores texture alpha and shows the
     * glass's transparent pixels as black. Move it to {@code translucent} so the panel actually blends through,
     * matching its {@code noOcclusion} block property.
     */
    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.GLASS.get(), RenderType.translucent()));
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CORE.get(), CompressionCoreScreen::new);
    }

    private static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, view, pos, tintIndex) -> {
            BigInteger level = BigInteger.ONE;
            if (view != null && pos != null
                    && view.getBlockEntity(pos) instanceof CompressionBlockEntity blockEntity) {
                level = blockEntity.getCompressionLevel();
            }
            return CompressionConfig.tintArgb(level);
        }, ModBlocks.COMPRESSED_COBBLESTONE.get());
    }

    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) ->
                        CompressionConfig.tintArgb(stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE)),
                ModItems.COMPRESSED_COBBLESTONE.get());
    }

    private ClientEvents() {
    }
}
