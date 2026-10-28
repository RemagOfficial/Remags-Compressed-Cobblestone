package com.remag.rcc.registry;

import com.mojang.serialization.Codec;
import com.remag.rcc.RCC;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.math.BigInteger;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTRAR =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RCC.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BigInteger>> COMPRESSION_LEVEL =
            REGISTRAR.registerComponentType("compression_level",
                    builder -> builder.persistent(
                            Codec.STRING.xmap(BigInteger::new, BigInteger::toString)));

    private ModDataComponents() {
    }
}
