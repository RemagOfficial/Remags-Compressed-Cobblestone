package com.remag.rcc.item;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RCC.MODID);

    /**
     * The only compressed cobblestone item. The tier is a data component on the stack, so one
     * registration covers every level; the per-level name and tint are derived from that component.
     */
    public static final DeferredItem<Item> COMPRESSED_COBBLESTONE =
            ITEMS.register("compressed_cobblestone",
                    () -> new CompressedCobblestoneItem(ModBlocks.COMPRESSED_COBBLESTONE.get(),
                            new Item.Properties()));

    // --- Compression Manipulator multiblock items ----------------------------------------------

    public static final DeferredItem<Item> COMPRESSION_FRAME =
            ITEMS.register("compression_frame",
                    () -> new BlockItem(ModBlocks.FRAME.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_GLASS =
            ITEMS.register("compression_glass",
                    () -> new BlockItem(ModBlocks.GLASS.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_PORT =
            ITEMS.register("compression_port",
                    () -> new BlockItem(ModBlocks.PORT.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_CORE =
            ITEMS.register("compression_core",
                    () -> new BlockItem(ModBlocks.CORE.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_INTERFACE =
            ITEMS.register("compression_interface",
                    () -> new BlockItem(ModBlocks.INTERFACE.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
