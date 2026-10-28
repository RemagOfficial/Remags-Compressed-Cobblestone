package com.remag.rcc.block;

import com.remag.rcc.RCC;
import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BceStorageBlock;
import com.remag.rcc.machine.CobblestoneGeneratorBlock;
import com.remag.rcc.machine.CompressionCoreBlock;
import com.remag.rcc.machine.CompressionAutoBlock;
import com.remag.rcc.machine.CompressionAutoDecompressorBlock;
import com.remag.rcc.machine.CompressionEditorBlock;
import com.remag.rcc.machine.DecompressorCoreBlock;
import com.remag.rcc.machine.GeneratorCoreBlock;
import com.remag.rcc.machine.CompressionFrameBlock;
import com.remag.rcc.machine.CompressionGlassBlock;
import com.remag.rcc.machine.CompressionInterfaceBlock;
import com.remag.rcc.machine.CompressionPortBlock;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RCC.MODID);

    
    public static final DeferredBlock<Block> COMPRESSED_COBBLESTONE =
            BLOCKS.register("compressed_cobblestone", CompressedCobblestoneBlock::new);


    
    public static final DeferredBlock<CompressionFrameBlock> FRAME =
            BLOCKS.register("compression_frame",
                    () -> new CompressionFrameBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()));

    
    public static final DeferredBlock<CompressionGlassBlock> GLASS =
            BLOCKS.register("compression_glass",
                    () -> new CompressionGlassBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.3F)
                            .noOcclusion()));

    
    public static final DeferredBlock<CompressionPortBlock> PORT =
            BLOCKS.register("compression_port",
                    () -> new CompressionPortBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()));

    
    public static final DeferredBlock<CompressionCoreBlock> CORE =
            BLOCKS.register("compression_core",
                    () -> new CompressionCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));

    
    public static final DeferredBlock<CompressionCoreBlock> COMPRESSION_CORE_2 =
            BLOCKS.register("compression_core_2",
                    () -> new CompressionCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion(),
                            CompressionBand.BAND_2));

    
    public static final Map<CompressionBand, DeferredBlock<CompressionCoreBlock>> COMPRESSION_CORES_BY_BAND =
            new HashMap<>();

    static {
        for (CompressionBand band : CompressionBand.values()) {
            if (band.index() < 3) {
                continue;
            }
            COMPRESSION_CORES_BY_BAND.put(band, BLOCKS.register("compression_core_" + band.index(),
                    () -> new CompressionCoreBlock(coreBlockProperties(), band)));
        }
    }

    
    private static BlockBehaviour.Properties coreBlockProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(8.0F, 12.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion();
    }

    
    public static final DeferredBlock<CompressionInterfaceBlock> INTERFACE =
            BLOCKS.register("compression_interface",
                    () -> new CompressionInterfaceBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()));

    
    public static final DeferredBlock<DecompressorCoreBlock> COMPRESSION_DECOMPRESSOR_CORE =
            BLOCKS.register("compression_decompressor_core",
                    () -> new DecompressorCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));

    
    public static final DeferredBlock<DecompressorCoreBlock> COMPRESSION_DECOMPRESSOR_CORE_2 =
            BLOCKS.register("compression_decompressor_core_2",
                    () -> new DecompressorCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion(),
                            CompressionBand.BAND_2));

    
    public static final DeferredBlock<DecompressorCoreBlock> COMPRESSION_DECOMPRESSOR_CORE_3 =
            BLOCKS.register("compression_decompressor_core_3",
                    () -> new DecompressorCoreBlock(coreBlockProperties(), CompressionBand.BAND_3));

    
    public static final DeferredBlock<CompressionAutoBlock> COMPRESSION_AUTO =
            BLOCKS.register("compression_auto",
                    () -> new CompressionAutoBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()));

    
    public static final DeferredBlock<CompressionAutoDecompressorBlock> COMPRESSION_AUTO_DECOMPRESSOR =
            BLOCKS.register("compression_auto_decompressor",
                    () -> new CompressionAutoDecompressorBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()));

    
    public static final DeferredBlock<CompressionEditorBlock> COMPRESSION_EDITOR =
            BLOCKS.register("compression_editor",
                    () -> new CompressionEditorBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()));


    
    public static final DeferredBlock<BcePipeBlock> BCE_PIPE =
            BLOCKS.register("bce_pipe",
                    () -> new BcePipeBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));

    
    public static final DeferredBlock<BceStorageBlock> BCE_STORAGE =
            BLOCKS.register("bce_storage",
                    () -> new BceStorageBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()));

    
    public static final DeferredBlock<BceStorageBlock> BCE_STORAGE_2 =
            BLOCKS.register("bce_storage_2",
                    () -> new BceStorageBlock(BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops(),
                            CompressionConfig.BCE_STORAGE_2_CAPACITY_LOG));

    
    public static final DeferredBlock<BceStorageBlock> BCE_STORAGE_3 =
            BLOCKS.register("bce_storage_3",
                    () -> new BceStorageBlock(BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops(),
                            CompressionConfig.BCE_STORAGE_3_CAPACITY_LOG));

    
    public static final Map<CompressionBand, DeferredBlock<BceStorageBlock>> BCE_STORAGES_BY_BAND =
            new HashMap<>();

    static {
        for (CompressionBand band : CompressionBand.values()) {
            if (band.index() < 3) {
                continue;
            }
            int bandSlot = band.index() - 3;
            String name = "bce_storage_" + (band.index() + 1);
            BCE_STORAGES_BY_BAND.put(band, BLOCKS.register(name,
                    () -> new BceStorageBlock(BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops(),
                            CompressionConfig.BCE_STORAGE_BAND_CAPACITY_LOGS[bandSlot])));
        }
    }

    
    public static final DeferredBlock<BceStorageBlock> CREATIVE_BCE_SOURCE =
            BLOCKS.register("creative_bce_source",
                    () -> new BceStorageBlock(BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops(),
                            CompressionBand.TOP.sinkCeilingLog(), true));


    
    public static final DeferredBlock<CobblestoneGeneratorBlock> COBBLESTONE_GENERATOR =
            BLOCKS.register("cobblestone_generator",
                    () -> new CobblestoneGeneratorBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()));

    
    public static final DeferredBlock<GeneratorCoreBlock> GENERATOR_CORE =
            BLOCKS.register("generator_core",
                    () -> new GeneratorCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));

    
    public static final DeferredBlock<GeneratorCoreBlock> GENERATOR_CORE_2 =
            BLOCKS.register("generator_core_2",
                    () -> new GeneratorCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion(),
                            CompressionBand.BAND_2));

    
    public static final Map<CompressionBand, DeferredBlock<GeneratorCoreBlock>> GENERATOR_CORES_BY_BAND =
            new HashMap<>();

    static {
        for (CompressionBand band : CompressionBand.values()) {
            if (band.index() < 3) {
                continue;
            }
            GENERATOR_CORES_BY_BAND.put(band, BLOCKS.register("generator_core_" + band.index(),
                    () -> new GeneratorCoreBlock(coreBlockProperties(), band)));
        }
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
