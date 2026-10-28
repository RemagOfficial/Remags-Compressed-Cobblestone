package com.remag.rcc.menu;

import com.remag.rcc.RCC;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, RCC.MODID);

    public static final Supplier<MenuType<CompressionCoreMenu>> CORE =
            MENUS.register("compression_core",
                    () -> IMenuTypeExtension.create(CompressionCoreMenu::client));

    public static final Supplier<MenuType<CompressionEditorMenu>> COMPRESSION_EDITOR =
            MENUS.register("compression_editor",
                    () -> IMenuTypeExtension.create(CompressionEditorMenu::client));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    private ModMenus() {
    }
}
