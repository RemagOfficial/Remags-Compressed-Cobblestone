package com.remag.rcc.block;

import com.remag.rcc.RCC;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeTier;
import com.remag.rcc.machine.CobblestoneGeneratorBlock;
import com.remag.rcc.machine.CompressionPortBlock;
import com.remag.rcc.machine.GeneratorCoreBlock;
import com.remag.rcc.machine.GeneratorCoreTier;
import com.remag.rcc.machine.GeneratorTier;
import com.remag.rcc.machine.PortType;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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
                        for (BigInteger level = BigInteger.ONE;
                             level.compareTo(CompressionConfig.MANUAL_MAX_LEVEL) <= 0;
                             level = level.add(BigInteger.ONE)) {
                            output.accept(levelStack(level));
                        }
                    })
                    .build()
    );

    
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RCC_MACHINES_TAB =
            TABS.register("rcc_machines_tab",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.rcc_machines_tab"))
                            .icon(() -> new ItemStack(ModItems.COMPRESSION_CORE.get()))
                            .displayItems((displayParams, output) -> {
                                output.accept(new ItemStack(ModItems.COMPRESSION_FRAME.get()));
                                output.accept(new ItemStack(ModItems.COMPRESSION_GLASS.get()));
                                output.accept(portStack(PortType.POWER));
                                output.accept(portStack(PortType.ITEM_IN));
                                output.accept(portStack(PortType.ITEM_OUT));
                                output.accept(portStack(PortType.SEED));
                                output.accept(new ItemStack(ModItems.COMPRESSION_INTERFACE.get()));
                                output.accept(new ItemStack(ModItems.COMPRESSION_CORE.get()));
                                output.accept(new ItemStack(ModItems.COMPRESSION_DECOMPRESSOR_CORE.get()));
                                output.accept(new ItemStack(ModItems.COMPRESSION_CORE_2.get()));
                                output.accept(new ItemStack(ModItems.COMPRESSION_DECOMPRESSOR_CORE_2.get()));
                                for (CompressionBand band : CompressionBand.values()) {
                                    if (band.index() < 3) {
                                        continue;
                                    }
                                    output.accept(new ItemStack(ModItems.COMPRESSION_CORES_BY_BAND.get(band).get()));
                                    if (band.hasDecompressor()) {
                                        output.accept(new ItemStack(ModItems.COMPRESSION_DECOMPRESSOR_CORE_3.get()));
                                    }
                                }
                                output.accept(new ItemStack(ModItems.COMPRESSION_AUTO.get()));
                                output.accept(new ItemStack(ModItems.COMPRESSION_AUTO_DECOMPRESSOR.get()));
                                output.accept(new ItemStack(ModItems.COMPRESSION_EDITOR.get()));
                                for (BcePipeTier tier : BcePipeTier.values()) {
                                    output.accept(pipeStack(tier));
                                }
                                output.accept(new ItemStack(ModItems.BCE_STORAGE.get()));
                                output.accept(new ItemStack(ModItems.BCE_STORAGE_2.get()));
                                output.accept(new ItemStack(ModItems.BCE_STORAGE_3.get()));
                                for (CompressionBand band : CompressionBand.values()) {
                                    if (band.index() < 3) {
                                        continue;
                                    }
                                    output.accept(new ItemStack(ModItems.BCE_STORAGES_BY_BAND.get(band).get()));
                                }
                                output.accept(new ItemStack(ModItems.CREATIVE_BCE_SOURCE.get()));
                                output.accept(new ItemStack(ModItems.WRENCH.get()));
                                for (GeneratorTier tier : GeneratorTier.values()) {
                                    output.accept(generatorStack(tier));
                                }
                                for (GeneratorCoreTier rung : GeneratorCoreTier.rungsFor(CompressionBand.BAND_1)) {
                                    output.accept(generatorCoreStack(ModItems.GENERATOR_CORE.get(), rung));
                                }
                                for (GeneratorCoreTier rung : GeneratorCoreTier.rungsFor(CompressionBand.BAND_2)) {
                                    output.accept(generatorCoreStack(ModItems.GENERATOR_CORE_2.get(), rung));
                                }
                                for (CompressionBand band : CompressionBand.values()) {
                                    if (band.index() < 3) {
                                        continue;
                                    }
                                    Item coreItem = ModItems.GENERATOR_CORES_BY_BAND.get(band).get();
                                    for (GeneratorCoreTier rung : GeneratorCoreTier.rungsFor(band)) {
                                        output.accept(generatorCoreStack(coreItem, rung));
                                    }
                                }
                            })
                            .build()
            );

    
    private static ItemStack levelStack(BigInteger level) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), level);
        return stack;
    }

    
    private static ItemStack portStack(PortType type) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSION_PORT.get());
        stack.set(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(CompressionPortBlock.PORT_TYPE, type));
        return stack;
    }

    
    private static ItemStack pipeStack(BcePipeTier tier) {
        ItemStack stack = new ItemStack(ModItems.BCE_PIPE.get());
        stack.set(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(BcePipeBlock.TIER, tier));
        return stack;
    }

    
    private static ItemStack generatorStack(GeneratorTier tier) {
        ItemStack stack = new ItemStack(ModItems.COBBLESTONE_GENERATOR.get());
        stack.set(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(CobblestoneGeneratorBlock.TIER, tier));
        return stack;
    }

    
    private static ItemStack generatorCoreStack(Item coreItem, GeneratorCoreTier rung) {
        ItemStack stack = new ItemStack(coreItem);
        stack.set(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(GeneratorCoreBlock.RUNG, rung.index()));
        return stack;
    }
}
