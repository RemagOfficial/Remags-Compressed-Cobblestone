package com.remag.rcc.block;

import com.remag.rcc.RCC;
import com.remag.rcc.machine.CompressionCoreBlock;
import com.remag.rcc.machine.CompressionFrameBlock;
import com.remag.rcc.machine.CompressionGlassBlock;
import com.remag.rcc.machine.CompressionInterfaceBlock;
import com.remag.rcc.machine.CompressionPortBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RCC.MODID);

    /**
     * The only compressed cobblestone block. Its block entity stores the level, so one registration
     * covers every tier instead of one block per {@link com.remag.rcc.registry.CompressionConfig#MANUAL_MAX_LEVEL}.
     */
    public static final DeferredBlock<Block> COMPRESSED_COBBLESTONE =
            BLOCKS.register("compressed_cobblestone", CompressedCobblestoneBlock::new);

    // --- Compression Manipulator multiblock ----------------------------------------------------

    /** Solid edge / corner cube of the 5x5x5 shell. */
    public static final DeferredBlock<CompressionFrameBlock> FRAME =
            BLOCKS.register("compression_frame",
                    () -> new CompressionFrameBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()));

    /** Glass-like face panel of the shell (non-occluding so the interior reads through). */
    public static final DeferredBlock<CompressionGlassBlock> GLASS =
            BLOCKS.register("compression_glass",
                    () -> new CompressionGlassBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.3F)
                            .noOcclusion()));

    /** Connection point; the {@code port_type} state selects power / item-in / item-out behaviour. */
    public static final DeferredBlock<CompressionPortBlock> PORT =
            BLOCKS.register("compression_port",
                    () -> new CompressionPortBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()));

    /** The controller at the exact centre of the structure. */
    public static final DeferredBlock<CompressionCoreBlock> CORE =
            BLOCKS.register("compression_core",
                    () -> new CompressionCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            // The core uses a non-cube (anvil) model. Without noOcclusion its full-cube block
                            // shape culls the faces of adjacent blocks (e.g. the ground beneath), leaving a void
                            // where the model doesn't fill the cell. Match the glass: don't occlude neighbours.
                            .noOcclusion()));

    /** Face panel that opens the machine GUI; the core itself is sealed and not directly clickable. */
    public static final DeferredBlock<CompressionInterfaceBlock> INTERFACE =
            BLOCKS.register("compression_interface",
                    () -> new CompressionInterfaceBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
