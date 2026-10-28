package com.remag.rcc.event;

import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.block.entity.CompressionBlockEntity;
import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.client.CompressionCoreRenderer;
import com.remag.rcc.client.CompressionCoreScreen;
import com.remag.rcc.client.CompressionEditorScreen;
import com.remag.rcc.client.ClientTowerListTooltip;
import com.remag.rcc.client.ClientTowerTooltip;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeTier;
import com.remag.rcc.menu.ModMenus;
import com.remag.rcc.registry.Banded;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;
import com.remag.rcc.registry.ModDataComponents;
import com.remag.rcc.registry.TowerListTooltipComponent;
import com.remag.rcc.registry.TowerTooltipComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public final class ClientEvents {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientEvents::onRegisterBlockColors);
        modEventBus.addListener(ClientEvents::onRegisterItemColors);
        modEventBus.addListener(ClientEvents::onRegisterMenuScreens);
        modEventBus.addListener(ClientEvents::onRegisterRenderers);
        modEventBus.addListener(ClientEvents::onRegisterTooltipComponents);
        modEventBus.addListener(ClientEvents::onClientSetup);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onItemTooltip);
    }

    private static void onRegisterTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(TowerTooltipComponent.class, ClientTowerTooltip::new);
        event.register(TowerListTooltipComponent.class, ClientTowerListTooltip::new);
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(Items.COBBLESTONE)) {
            event.getToolTip().add(Component.translatable("tooltip.rcc.bce", DisplayNames.bceValueText(BigInteger.ZERO))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.COMPRESSION_CORE.get(), CompressionCoreRenderer::new);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.GLASS.get(), RenderType.translucent()));
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CORE.get(), CompressionCoreScreen::new);
        event.register(ModMenus.COMPRESSION_EDITOR.get(), CompressionEditorScreen::new);
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

        event.register((state, view, pos, tintIndex) -> state.getBlock() instanceof Banded banded
                        ? CompressionConfig.bandTintArgb(banded.band())
                        : 0xFFFFFFFF,
                coreBlocks());

        event.register((state, view, pos, tintIndex) ->
                        CompressionConfig.PIPE_TINT_ARGB[state.getValue(BcePipeBlock.TIER).ordinal()],
                ModBlocks.BCE_PIPE.get());
    }

    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) ->
                        CompressionConfig.tintArgb(stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE)),
                ModItems.COMPRESSED_COBBLESTONE.get());

        event.register((stack, tintIndex) -> stack.getItem() instanceof BlockItem blockItem
                        && blockItem.getBlock() instanceof Banded banded
                        ? CompressionConfig.bandTintArgb(banded.band())
                        : 0xFFFFFFFF,
                coreItems());

        event.register((stack, tintIndex) -> {
            BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
            BcePipeTier tier = props == null ? null : props.get(BcePipeBlock.TIER);
            return CompressionConfig.PIPE_TINT_ARGB[tier == null ? 0 : tier.ordinal()];
        }, ModItems.BCE_PIPE.get());
    }

    private static Block[] coreBlocks() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(ModBlocks.CORE.get());
        blocks.add(ModBlocks.COMPRESSION_CORE_2.get());
        ModBlocks.COMPRESSION_CORES_BY_BAND.values().forEach(holder -> blocks.add(holder.get()));
        blocks.add(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get());
        blocks.add(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_2.get());
        blocks.add(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_3.get());
        blocks.add(ModBlocks.GENERATOR_CORE.get());
        blocks.add(ModBlocks.GENERATOR_CORE_2.get());
        ModBlocks.GENERATOR_CORES_BY_BAND.values().forEach(holder -> blocks.add(holder.get()));
        return blocks.toArray(new Block[0]);
    }

    private static Item[] coreItems() {
        List<Item> items = new ArrayList<>();
        items.add(ModItems.COMPRESSION_CORE.get());
        items.add(ModItems.COMPRESSION_CORE_2.get());
        ModItems.COMPRESSION_CORES_BY_BAND.values().forEach(holder -> items.add(holder.get()));
        items.add(ModItems.COMPRESSION_DECOMPRESSOR_CORE.get());
        items.add(ModItems.COMPRESSION_DECOMPRESSOR_CORE_2.get());
        items.add(ModItems.COMPRESSION_DECOMPRESSOR_CORE_3.get());
        items.add(ModItems.GENERATOR_CORE.get());
        items.add(ModItems.GENERATOR_CORE_2.get());
        ModItems.GENERATOR_CORES_BY_BAND.values().forEach(holder -> items.add(holder.get()));
        return items.toArray(new Item[0]);
    }

    private ClientEvents() {
    }
}
