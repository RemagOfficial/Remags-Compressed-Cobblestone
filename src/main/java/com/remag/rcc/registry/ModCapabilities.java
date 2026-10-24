package com.remag.rcc.registry;

import com.remag.rcc.RCC;
import com.remag.rcc.energy.IBceStorage;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/** Custom NeoForge block capabilities contributed by this mod (beyond the built-in energy/item caps). */
public final class ModCapabilities {
    /**
     * Sided access to an {@link IBceStorage} - the only BCE API in the mod, so anything that holds BCE and anything
     * that wants BCE speak the same language.
     *
     * <p>Implemented by the two stores: the Auto Decompressor (a producer view, whose buffer is drained by pipes) and
     * the standalone {@link com.remag.rcc.machine.BceStorageBlock} tank. Consumed by
     * {@link com.remag.rcc.energy.BceNetwork}, whose extract ends query every block they touch once per endpoint
     * refresh - so {@code rcc:bce_storage} is also the integration point for third-party BCE machinery.
     *
     * <p>Pipes are deliberately <em>not</em> stores and register a null handler; see {@link
     * com.remag.rcc.event.CommonEvents#registerCapabilities}. The context is the {@link Direction} face, matching the
     * built-in item/energy block capabilities.
     */
    public static final BlockCapability<IBceStorage, @Nullable Direction> BCE =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(RCC.MODID, "bce_storage"), IBceStorage.class);

    private ModCapabilities() {
    }
}
