package com.remag.rcc.registry;

import com.remag.rcc.RCC;
import com.remag.rcc.energy.IBceStorage;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

public final class ModCapabilities {
    public static final BlockCapability<IBceStorage, @Nullable Direction> BCE =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(RCC.MODID, "bce_storage"), IBceStorage.class);

    private ModCapabilities() {
    }
}
