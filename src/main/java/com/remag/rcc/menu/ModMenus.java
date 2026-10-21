package com.remag.rcc.menu;

import com.remag.rcc.RCC;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Registers the Compression Manipulator's {@link MenuType}. */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, RCC.MODID);

    /**
     * The core menu. Built through {@link IMenuTypeExtension#create} so the client can reconstruct it from the
     * {@link net.minecraft.core.BlockPos} that {@code player.openMenu(provider, pos)} writes into the opening
     * buffer (vanilla {@code MenuType} constructors cannot read that extra data).
     */
    public static final Supplier<MenuType<CompressionCoreMenu>> CORE =
            MENUS.register("compression_core",
                    () -> IMenuTypeExtension.create(CompressionCoreMenu::client));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    private ModMenus() {
    }
}
