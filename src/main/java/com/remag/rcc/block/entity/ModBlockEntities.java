package com.remag.rcc.block.entity;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.machine.BcePipeBlockEntity;
import com.remag.rcc.machine.BceStorageBlockEntity;
import com.remag.rcc.machine.CobblestoneGeneratorBlockEntity;
import com.remag.rcc.machine.CompressionCoreBlockEntity;
import com.remag.rcc.machine.CompressionAutoBlockEntity;
import com.remag.rcc.machine.CompressionAutoDecompressorBlockEntity;
import com.remag.rcc.machine.CompressionEditorBlockEntity;
import com.remag.rcc.machine.CompressionPortBlockEntity;
import com.remag.rcc.machine.DecompressorCoreBlockEntity;
import com.remag.rcc.machine.GeneratorCoreBlockEntity;
import com.remag.rcc.registry.CompressionBand;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RCC.MODID);

    public static final Supplier<BlockEntityType<CompressionBlockEntity>> COMPRESSION =
            BLOCK_ENTITIES.register("compressed_cobblestone",
                    () -> BlockEntityType.Builder.of(CompressionBlockEntity::new,
                            ModBlocks.COMPRESSED_COBBLESTONE.get()).build(null));

    public static final Supplier<BlockEntityType<CompressionPortBlockEntity>> COMPRESSION_PORT =
            BLOCK_ENTITIES.register("compression_port",
                    () -> BlockEntityType.Builder.of(CompressionPortBlockEntity::new,
                            ModBlocks.PORT.get()).build(null));

    public static final Supplier<BlockEntityType<CompressionCoreBlockEntity>> COMPRESSION_CORE =
            BLOCK_ENTITIES.register("compression_core",
                    () -> BlockEntityType.Builder.of(CompressionCoreBlockEntity::new,
                            allBlocks(ModBlocks.CORE.get(), ModBlocks.COMPRESSION_CORE_2.get(),
                                    ModBlocks.COMPRESSION_CORES_BY_BAND)).build(null));

    public static final Supplier<BlockEntityType<DecompressorCoreBlockEntity>> COMPRESSION_DECOMPRESSOR_CORE =
            BLOCK_ENTITIES.register("compression_decompressor_core",
                    () -> BlockEntityType.Builder.of(DecompressorCoreBlockEntity::new,
                            ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get(),
                            ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_2.get(),
                            ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_3.get()).build(null));

    public static final Supplier<BlockEntityType<CompressionAutoBlockEntity>> COMPRESSION_AUTO =
            BLOCK_ENTITIES.register("compression_auto",
                    () -> BlockEntityType.Builder.of(CompressionAutoBlockEntity::new,
                            ModBlocks.COMPRESSION_AUTO.get()).build(null));

    public static final Supplier<BlockEntityType<CompressionAutoDecompressorBlockEntity>> COMPRESSION_AUTO_DECOMPRESSOR =
            BLOCK_ENTITIES.register("compression_auto_decompressor",
                    () -> BlockEntityType.Builder.of(CompressionAutoDecompressorBlockEntity::new,
                            ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR.get()).build(null));

    public static final Supplier<BlockEntityType<CompressionEditorBlockEntity>> COMPRESSION_EDITOR =
            BLOCK_ENTITIES.register("compression_editor",
                    () -> BlockEntityType.Builder.of(CompressionEditorBlockEntity::new,
                            ModBlocks.COMPRESSION_EDITOR.get()).build(null));

    public static final Supplier<BlockEntityType<BcePipeBlockEntity>> BCE_PIPE =
            BLOCK_ENTITIES.register("bce_pipe",
                    () -> BlockEntityType.Builder.of(BcePipeBlockEntity::new,
                            ModBlocks.BCE_PIPE.get()).build(null));

    public static final Supplier<BlockEntityType<BceStorageBlockEntity>> BCE_STORAGE =
            BLOCK_ENTITIES.register("bce_storage",
                    () -> BlockEntityType.Builder.of(BceStorageBlockEntity::new,
                    withCreativeSource(allBlocks(ModBlocks.BCE_STORAGE.get(), ModBlocks.BCE_STORAGE_2.get(),
                            ModBlocks.BCE_STORAGES_BY_BAND))).build(null));

    public static final Supplier<BlockEntityType<CobblestoneGeneratorBlockEntity>> COBBLESTONE_GENERATOR =
            BLOCK_ENTITIES.register("cobblestone_generator",
                    () -> BlockEntityType.Builder.of(CobblestoneGeneratorBlockEntity::new,
                            ModBlocks.COBBLESTONE_GENERATOR.get()).build(null));

    public static final Supplier<BlockEntityType<GeneratorCoreBlockEntity>> GENERATOR_CORE =
            BLOCK_ENTITIES.register("generator_core",
                    () -> BlockEntityType.Builder.of(GeneratorCoreBlockEntity::new,
                            allBlocks(ModBlocks.GENERATOR_CORE.get(), ModBlocks.GENERATOR_CORE_2.get(),
                                    ModBlocks.GENERATOR_CORES_BY_BAND)).build(null));

    private static Block[] allBlocks(Block first, Block second,
                                     Map<CompressionBand, ? extends DeferredBlock<? extends Block>> banded) {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(first, second),
                        Arrays.stream(CompressionBand.values())
                                .filter(band -> band.index() >= 3)
                                .map(band -> banded.get(band).get()))
                .toArray(Block[]::new);
    }

    private static Block[] withCreativeSource(Block[] tanks) {
        Block[] all = java.util.Arrays.copyOf(tanks, tanks.length + 1);
        all[tanks.length] = ModBlocks.CREATIVE_BCE_SOURCE.get();
        return all;
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

    private ModBlockEntities() {
    }
}
