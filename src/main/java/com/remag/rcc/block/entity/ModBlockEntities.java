package com.remag.rcc.block.entity;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.machine.CompressionCoreBlockEntity;
import com.remag.rcc.machine.CompressionPortBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

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
                            ModBlocks.CORE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

    private ModBlockEntities() {
    }
}
