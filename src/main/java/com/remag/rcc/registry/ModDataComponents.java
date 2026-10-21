package com.remag.rcc.registry;

import com.mojang.serialization.Codec;
import com.remag.rcc.RCC;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.math.BigInteger;

/**
 * The single piece of per-level data: the compression level. It is the one source of truth used by
 * the {@link com.remag.rcc.item.CompressedCobblestoneItem item} (data component), the
 * {@link com.remag.rcc.block.entity.CompressionBlockEntity block entity} (drop/pick/sync), runtime
 * tinting and per-level naming. Registering this instead of one block per level is what lets
 * {@link CompressionConfig#MANUAL_MAX_LEVEL} scale without thousands of registry entries.
 */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTRAR =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RCC.MODID);

    /**
     * Compression tier, 1-based. Absent on a stack is treated as tier 1. Stored as a {@link BigInteger}
     * (serialised as its decimal string) so tiers can climb far past {@code int}/{@code long} toward the
     * googolplex goal without a later data-format migration.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BigInteger>> COMPRESSION_LEVEL =
            REGISTRAR.registerComponentType("compression_level",
                    builder -> builder.persistent(
                            Codec.STRING.xmap(BigInteger::new, BigInteger::toString)));

    private ModDataComponents() {
    }
}
