package com.remag.rcc.event;

import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.block.entity.CompressionBlockEntity;
import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.client.CompressionCoreRenderer;
import com.remag.rcc.client.CompressionCoreScreen;
import com.remag.rcc.client.ClientTowerTooltip;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeTier;
import com.remag.rcc.menu.ModMenus;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.DisplayNames;
import com.remag.rcc.registry.ModDataComponents;
import com.remag.rcc.registry.TowerTooltipComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.math.BigInteger;

/**
 * Client-only render wiring. Two jobs, both of which exist because one registration covers many variants:
 *
 * <ul>
 *   <li><b>runtime tinting</b> for the compressed cobblestone, whose tiers are one block/one item distinguished only
 *       by the {@code rcc:compression_level} data component, so the per-level colour that used to be baked into 10k
 *       models is computed at render time from {@link CompressionConfig#tintArgb(java.math.BigInteger)} - placed
 *       blocks read the level off the {@link CompressionBlockEntity}, items (inventory, hand, dropped, block item)
 *       read it off the stack, and the plain-cube model has no baked colour for the tint to replace. The ramp is no
 *       longer grayscale: it runs cobblestone-grey to black to white and then down the rainbow, one hue per
 *       {@link com.remag.rcc.registry.CompressionBand};</li>
 *   <li><b>the BCE pipe's tier property</b>, which is how the single {@code rcc:bce_pipe} item renders four different
 *       inventory models. The pipe is deliberately <em>not</em> tinted any more: its tiers are textures and its
 *       extract/insert ends are geometry, so {@code blockstates/bce_pipe.json} selects the art per state and nothing
 *       here multiplies a colour into it;</li>
 *   <li><b>the drawn power tower</b>, the client half of {@link TowerTooltipComponent}. A machine's BCE figure is
 *       {@code 9^(10^12)}-class - an exponent of an exponent - and Unicode only superscripts once, so the tooltips that
 *       carry it draw it instead ({@link ClientTowerTooltip}).</li>
 * </ul>
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
        modEventBus.addListener(ClientEvents::onRegisterTooltipComponents);
        modEventBus.addListener(ClientEvents::onClientSetup);
        // Vanilla cobblestone is not ours to override, so its BCE readout rides the game-bus tooltip event; the
        // compressed tiers get theirs from CompressedCobblestoneItem.getTooltipImage.
        NeoForge.EVENT_BUS.addListener(ClientEvents::onItemTooltip);
    }

    /**
     * Teaches the tooltip renderer what to do with a {@link TowerTooltipComponent}. Without a factory
     * {@code ClientTooltipComponent.create} throws {@code Unknown TooltipComponent}, so this registration is what makes
     * the machine items' {@code getTooltipImage} payload safe to send at all.
     */
    private static void onRegisterTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(TowerTooltipComponent.class, ClientTowerTooltip::new);
    }

    /** The base unit of the whole economy: one piece of vanilla cobblestone is worth exactly 1 BCE. */
    private static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(Items.COBBLESTONE)) {
            event.getToolTip().add(Component.translatable("tooltip.rcc.bce", DisplayNames.bceValueText(BigInteger.ZERO))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** The core renders its (client-synced) seed as a large spinning 3D cube via this block entity renderer. */
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.COMPRESSION_CORE.get(), CompressionCoreRenderer::new);
    }

    /**
     * Blocks bake into the {@code solid} render layer by default, which ignores texture alpha and shows the
     * glass's transparent pixels as black. Move it to {@code translucent} so the panel actually blends through,
     * matching its {@code noOcclusion} block property.
     *
     * <p>Also registers the pipe's tier as an item-model predicate: both tiers are one item, and in 1.21.1 the
     * {@code block_state} component only influences placement, so without this the inventory would show every pipe as
     * tier 1. The value is the tier ordinal, which is what the {@code overrides} datagen writes into
     * {@code models/item/bce_pipe.json} compare against.
     */
    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GLASS.get(), RenderType.translucent());
            ItemProperties.register(ModItems.BCE_PIPE.get(), BcePipeBlock.TIER_ITEM_PROPERTY, (stack, context, level, entity) -> {
                BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
                BcePipeTier tier = props == null ? null : props.get(BcePipeBlock.TIER);
                // No component at all (a bare /give, a crafted stack before the component was set) reads as tier 1,
                // which is also the block's own default, so the item and the placed block agree.
                return tier == null ? 0.0F : (float) tier.ordinal();
            });
        });
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

        // The BCE tank is a plain frame cube whose contents are read in the GUI, so its tint stays static;
        // the pipe gets no handler at all - tier is a texture and mode is geometry, both chosen by the block state.
    }

    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) ->
                        CompressionConfig.tintArgb(stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE)),
                ModItems.COMPRESSED_COBBLESTONE.get());
    }

    private ClientEvents() {
    }
}
