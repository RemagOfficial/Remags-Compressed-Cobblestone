package com.remag.rcc.item;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.registry.CompressionBand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RCC.MODID);

    
    public static final DeferredItem<Item> COMPRESSED_COBBLESTONE =
            ITEMS.register("compressed_cobblestone",
                    () -> new CompressedCobblestoneItem(ModBlocks.COMPRESSED_COBBLESTONE.get(),
                            new Item.Properties()));


    public static final DeferredItem<Item> COMPRESSION_FRAME =
            ITEMS.register("compression_frame",
                    () -> new BlockItem(ModBlocks.FRAME.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_GLASS =
            ITEMS.register("compression_glass",
                    () -> new BlockItem(ModBlocks.GLASS.get(), new Item.Properties()));

    
    public static final DeferredItem<Item> COMPRESSION_PORT =
            ITEMS.register("compression_port",
                    () -> new CompressionPortItem(ModBlocks.PORT.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_CORE =
            ITEMS.register("compression_core",
                    () -> new BlockItem(ModBlocks.CORE.get(), new Item.Properties()));

    
    public static final DeferredItem<Item> COMPRESSION_CORE_2 =
            ITEMS.register("compression_core_2",
                    () -> new BlockItem(ModBlocks.COMPRESSION_CORE_2.get(), new Item.Properties()));

    
    public static final Map<CompressionBand, DeferredItem<Item>> COMPRESSION_CORES_BY_BAND = new EnumMap<>(CompressionBand.class);

    
    public static final Map<CompressionBand, DeferredItem<Item>> GENERATOR_CORES_BY_BAND = new EnumMap<>(CompressionBand.class);

    
    public static final Map<CompressionBand, DeferredItem<Item>> BCE_STORAGES_BY_BAND = new EnumMap<>(CompressionBand.class);

    static {
        for (CompressionBand band : CompressionBand.values()) {
            if (band.index() < 3) {
                continue;
            }
            int n = band.index();
            COMPRESSION_CORES_BY_BAND.put(band, ITEMS.register("compression_core_" + n,
                    () -> new BlockItem(ModBlocks.COMPRESSION_CORES_BY_BAND.get(band).get(), new Item.Properties())));
            GENERATOR_CORES_BY_BAND.put(band, ITEMS.register("generator_core_" + n,
                    () -> new GeneratorCoreItem(ModBlocks.GENERATOR_CORES_BY_BAND.get(band).get(), new Item.Properties())));
            BCE_STORAGES_BY_BAND.put(band, ITEMS.register("bce_storage_" + (n + 1),
                    () -> new BlockItem(ModBlocks.BCE_STORAGES_BY_BAND.get(band).get(), new Item.Properties())));
        }
    }

    public static final DeferredItem<Item> COMPRESSION_DECOMPRESSOR_CORE =
            ITEMS.register("compression_decompressor_core",
                    () -> new BlockItem(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get(), new Item.Properties()));

    
    public static final DeferredItem<Item> COMPRESSION_DECOMPRESSOR_CORE_2 =
            ITEMS.register("compression_decompressor_core_2",
                    () -> new BlockItem(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_2.get(), new Item.Properties()));

    
    public static final DeferredItem<Item> COMPRESSION_DECOMPRESSOR_CORE_3 =
            ITEMS.register("compression_decompressor_core_3",
                    () -> new BlockItem(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_3.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_INTERFACE =
            ITEMS.register("compression_interface",
                    () -> new BlockItem(ModBlocks.INTERFACE.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_AUTO =
            ITEMS.register("compression_auto",
                    () -> new BlockItem(ModBlocks.COMPRESSION_AUTO.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_AUTO_DECOMPRESSOR =
            ITEMS.register("compression_auto_decompressor",
                    () -> new BlockItem(ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR.get(), new Item.Properties()));

    
    public static final DeferredItem<Item> COMPRESSION_EDITOR =
            ITEMS.register("compression_editor",
                    () -> new BlockItem(ModBlocks.COMPRESSION_EDITOR.get(), new Item.Properties()));


    
    public static final DeferredItem<Item> BCE_PIPE =
            ITEMS.register("bce_pipe",
                    () -> new BcePipeItem(ModBlocks.BCE_PIPE.get(), new Item.Properties()));

    public static final DeferredItem<Item> BCE_STORAGE =
            ITEMS.register("bce_storage",
                    () -> new BlockItem(ModBlocks.BCE_STORAGE.get(), new Item.Properties()));

    
    public static final DeferredItem<Item> BCE_STORAGE_2 =
            ITEMS.register("bce_storage_2",
                    () -> new BlockItem(ModBlocks.BCE_STORAGE_2.get(), new Item.Properties()));

    
    public static final DeferredItem<Item> BCE_STORAGE_3 =
            ITEMS.register("bce_storage_3",
                    () -> new BlockItem(ModBlocks.BCE_STORAGE_3.get(), new Item.Properties()));

    
    public static final DeferredItem<Item> CREATIVE_BCE_SOURCE =
            ITEMS.register("creative_bce_source",
                    () -> new BlockItem(ModBlocks.CREATIVE_BCE_SOURCE.get(), new Item.Properties()));

    
    public static final DeferredItem<Item> COBBLESTONE_GENERATOR =
            ITEMS.register("cobblestone_generator",
                    () -> new CobblestoneGeneratorItem(ModBlocks.COBBLESTONE_GENERATOR.get(), new Item.Properties()));

    
    public static final DeferredItem<Item> GENERATOR_CORE =
            ITEMS.register("generator_core",
                    () -> new GeneratorCoreItem(ModBlocks.GENERATOR_CORE.get(), new Item.Properties()));

    
    public static final DeferredItem<Item> GENERATOR_CORE_2 =
            ITEMS.register("generator_core_2",
                    () -> new GeneratorCoreItem(ModBlocks.GENERATOR_CORE_2.get(), new Item.Properties()));

    
    public static final DeferredItem<Item> WRENCH =
            ITEMS.register("wrench", () -> new Item(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
