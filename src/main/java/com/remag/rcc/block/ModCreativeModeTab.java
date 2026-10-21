package com.remag.rcc.block;

import com.remag.rcc.RCC;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.CompressionPortBlock;
import com.remag.rcc.machine.PortType;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.math.BigInteger;

public class ModCreativeModeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RCC.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RCC_TAB = TABS.register("rcc_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rcctab"))
                    .icon(() -> levelStack(BigInteger.ONE))
                    .displayItems((displayParams, output) -> {
                        // One entry per tier, all sharing the single item but differing by the
                        // compression_level data component. The per-tier name/tint come from that.
                        // The manual 3x3 chain tops out at MANUAL_MAX_LEVEL; bigger tiers come from
                        // the Compression Manipulator multiblock and are not listed here.
                        for (BigInteger level = BigInteger.ONE;
                             level.compareTo(CompressionConfig.MANUAL_MAX_LEVEL) <= 0;
                             level = level.add(BigInteger.ONE)) {
                            output.accept(levelStack(level));
                        }
                        // Compression Manipulator structure blocks (placeholder multiblock pieces).
                        output.accept(new ItemStack(ModItems.COMPRESSION_FRAME.get()));
                        output.accept(new ItemStack(ModItems.COMPRESSION_GLASS.get()));
                        output.accept(portStack(PortType.POWER));
                        output.accept(portStack(PortType.ITEM_IN));
                        output.accept(portStack(PortType.ITEM_OUT));
                        output.accept(new ItemStack(ModItems.COMPRESSION_INTERFACE.get()));
                        output.accept(new ItemStack(ModItems.COMPRESSION_CORE.get()));
                    })
                    .build()
    );

    /** A creative-tab stack for the given tier, carrying the compression_level component. */
    private static ItemStack levelStack(BigInteger level) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), level);
        return stack;
    }

    /** A port stack preset to the given {@link PortType}; {@code getStateForPlacement} reads it back. */
    private static ItemStack portStack(PortType type) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSION_PORT.get());
        stack.set(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(CompressionPortBlock.PORT_TYPE, type));
        return stack;
    }
}
