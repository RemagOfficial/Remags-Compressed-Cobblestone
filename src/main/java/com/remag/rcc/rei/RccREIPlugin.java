package com.remag.rcc.rei;

import com.remag.rcc.item.ModItems;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@REIPluginClient
public class RccREIPlugin implements REIClientPlugin {

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new CompressionCategory());
        registry.add(new ManipulatorCategory());
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (Display display : buildExamples()) {
            registry.add(display);
        }
        for (Display display : buildManipulatorExamples()) {
            registry.add(display);
        }
        registerInfo(registry);
    }

    private static void registerInfo(DisplayRegistry registry) {
        registry.add(info(new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get()),
                Component.translatable("jei.rcc.info.compressed.line1"),
                Component.translatable("jei.rcc.info.compressed.line2"),
                Component.translatable("jei.rcc.info.compressed.line3"),
                Component.translatable("jei.rcc.info.compressed.line4")));
        registry.add(info(new ItemStack(ModItems.COMPRESSION_CORE.get()),
                Component.translatable("jei.rcc.info.core.line1"),
                Component.translatable("jei.rcc.info.core.line2"),
                Component.translatable("jei.rcc.info.core.line3"),
                Component.translatable("jei.rcc.info.core.line4")));
        registry.add(info(new ItemStack(ModItems.COMPRESSION_INTERFACE.get()),
                Component.translatable("jei.rcc.info.interface.line1"),
                Component.translatable("jei.rcc.info.interface.line2")));
        registry.add(info(new ItemStack(ModItems.COMPRESSION_PORT.get()),
                Component.translatable("jei.rcc.info.port.line1"),
                Component.translatable("jei.rcc.info.port.line2"),
                Component.translatable("jei.rcc.info.port.line3"),
                Component.translatable("jei.rcc.info.port.line4")));
    }

    private static DefaultInformationDisplay info(ItemStack stack, Component... lines) {
        return DefaultInformationDisplay.createFromEntry(ReiItems.entry(stack), stack.getHoverName())
                .lines(lines);
    }

    private static List<Display> buildExamples() {
        List<Display> displays = new ArrayList<>();
        displays.add(compressExample(BigInteger.ONE));
        displays.add(compressExample(BigInteger.TWO));
        displays.add(compressExample(BigInteger.valueOf(3)));
        displays.add(decompressExample(BigInteger.valueOf(3)));
        displays.add(decompressExample(BigInteger.TWO));
        displays.add(decompressExample(BigInteger.ONE));
        return displays;
    }

    private static CompressionDisplay compressExample(BigInteger tier) {
        EntryIngredient input = EntryIngredient.of(ReiItems.entry(ReiItems.levelStack(tier)));
        List<EntryIngredient> inputs = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            inputs.add(input);
        }
        return new CompressionDisplay(inputs, List.of(EntryIngredient.of(
                ReiItems.entry(ReiItems.levelStack(tier.add(BigInteger.ONE))))), craftingTableWorkstation());
    }

    private static CompressionDisplay decompressExample(BigInteger tier) {
        ItemStack outputStack = tier.compareTo(BigInteger.ONE) <= 0
                ? new ItemStack(Items.COBBLESTONE)
                : ReiItems.levelStack(tier.subtract(BigInteger.ONE));
        outputStack.setCount(9);
        return new CompressionDisplay(
                List.of(EntryIngredient.of(ReiItems.entry(ReiItems.levelStack(tier)))),
                List.of(EntryIngredient.of(ReiItems.entry(outputStack))), craftingTableWorkstation());
    }

    private static List<EntryIngredient> craftingTableWorkstation() {
        return List.of(ReiItems.ingredientOf(Items.CRAFTING_TABLE));
    }

    private static List<Display> buildManipulatorExamples() {
        List<Display> displays = new ArrayList<>();
        for (CompressionBand band : new CompressionBand[]{CompressionBand.BAND_1, CompressionBand.BAND_2}) {
            BigInteger minSeed = band.minInputTier();
            displays.add(manipulatorExample(minSeed));
            displays.add(manipulatorExample(minSeed.multiply(BigInteger.TWO)));
        }
        return displays;
    }

    private static ManipulatorDisplay manipulatorExample(BigInteger seedTier) {
        BigInteger outTier = seedTier.shiftLeft(1);
        return new ManipulatorDisplay(
                List.of(EntryIngredient.of(ReiItems.entry(ReiItems.levelStack(seedTier)))),
                List.of(EntryIngredient.of(ReiItems.entry(ReiItems.levelStack(outTier)))),
                List.of(ReiItems.ingredientOfAll(ReiItems.allCompressionCores())),
                CompressionConfig.ENERGY_PER_CRAFT,
                CompressionConfig.requiredMassLog(seedTier));
    }
}
