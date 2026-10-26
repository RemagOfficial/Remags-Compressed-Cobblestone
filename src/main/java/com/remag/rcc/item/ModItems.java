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

    /** The one port item; the kind it places as comes from the stack's {@code block_state} component (defaults to POWER). */
    public static final DeferredItem<Item> COMPRESSION_PORT =
            ITEMS.register("compression_port",
                    () -> new CompressionPortItem(ModBlocks.PORT.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_CORE =
            ITEMS.register("compression_core",
                    () -> new BlockItem(ModBlocks.CORE.get(), new Item.Properties()));

    /** The band-2 compressor core (7x7x7 shell); the block supplies the band, the item is a plain block item. */
    public static final DeferredItem<Item> COMPRESSION_CORE_2 =
            ITEMS.register("compression_core_2",
                    () -> new BlockItem(ModBlocks.COMPRESSION_CORE_2.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_DECOMPRESSOR_CORE =
            ITEMS.register("compression_decompressor_core",
                    () -> new BlockItem(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get(), new Item.Properties()));

    /** The band-2 decompressor core (5x5x5 descent shell); the block supplies the band. */
    public static final DeferredItem<Item> COMPRESSION_DECOMPRESSOR_CORE_2 =
            ITEMS.register("compression_decompressor_core_2",
                    () -> new BlockItem(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_2.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_INTERFACE =
            ITEMS.register("compression_interface",
                    () -> new BlockItem(ModBlocks.INTERFACE.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_AUTO =
            ITEMS.register("compression_auto",
                    () -> new BlockItem(ModBlocks.COMPRESSION_AUTO.get(), new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSION_AUTO_DECOMPRESSOR =
            ITEMS.register("compression_auto_decompressor",
                    () -> new BlockItem(ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR.get(), new Item.Properties()));

    // --- BCE logistics -------------------------------------------------------------------------

    /** The one pipe item; tier and mode come from the stack's {@code block_state} component. */
    public static final DeferredItem<Item> BCE_PIPE =
            ITEMS.register("bce_pipe",
                    () -> new BcePipeItem(ModBlocks.BCE_PIPE.get(), new Item.Properties()));

    public static final DeferredItem<Item> BCE_STORAGE =
            ITEMS.register("bce_storage",
                    () -> new BlockItem(ModBlocks.BCE_STORAGE.get(), new Item.Properties()));

    /** The tier-two tank; same block class, bigger pool (sized on the block, not the item). */
    public static final DeferredItem<Item> BCE_STORAGE_2 =
            ITEMS.register("bce_storage_2",
                    () -> new BlockItem(ModBlocks.BCE_STORAGE_2.get(), new Item.Properties()));

    /** The tier-three tank, sized to the band-2 multiblock; again just a BlockItem over its tank block. */
    public static final DeferredItem<Item> BCE_STORAGE_3 =
            ITEMS.register("bce_storage_3",
                    () -> new BlockItem(ModBlocks.BCE_STORAGE_3.get(), new Item.Properties()));

    /** The generator item; its rung comes from the stack's {@code block_state} component (defaults to BASE). */
    public static final DeferredItem<Item> COBBLESTONE_GENERATOR =
            ITEMS.register("cobblestone_generator",
                    () -> new CobblestoneGeneratorItem(ModBlocks.COBBLESTONE_GENERATOR.get(), new Item.Properties()));

    /** The tier-1 generator multiblock's core; its rung comes from the stack's {@code block_state} component. */
    public static final DeferredItem<Item> GENERATOR_CORE =
            ITEMS.register("generator_core",
                    () -> new GeneratorCoreItem(ModBlocks.GENERATOR_CORE.get(), new Item.Properties()));

    /** The band-2 generator multiblock's core; the block supplies the band and the rung index rides the state. */
    public static final DeferredItem<Item> GENERATOR_CORE_2 =
            ITEMS.register("generator_core_2",
                    () -> new GeneratorCoreItem(ModBlocks.GENERATOR_CORE_2.get(), new Item.Properties()));

    /**
     * The configuration tool: right-click a BCE pipe with it to flip that end between extract and insert. It carries
     * no logic of its own - the blocks it can configure are found through the shared {@code c:tools/wrench}
     * convention tag (see {@link com.remag.rcc.registry.ModItemTags}), which our datagen also publishes this item
     * onto, so this wrench works on other mods' machines and theirs work on ours.
     */
    public static final DeferredItem<Item> WRENCH =
            ITEMS.register("wrench", () -> new Item(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
