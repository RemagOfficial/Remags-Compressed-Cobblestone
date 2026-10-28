package com.remag.rcc.jei;

import com.remag.rcc.RCC;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.ModDataComponents;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class RccJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(RCC.MODID, "compression");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new CompressionCategory(guiHelper));
        registration.addRecipeCategories(new ManipulatorCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CompressionCategory.TYPE, buildExamples());
        registration.addRecipes(ManipulatorCategory.TYPE, buildManipulatorExamples());
        registerInfo(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Blocks.CRAFTING_TABLE, CompressionCategory.TYPE);
        for (Item core : allCompressionCores()) {
            registration.addRecipeCatalyst(core, ManipulatorCategory.TYPE);
        }
    }

    private static List<Item> allCompressionCores() {
        List<Item> cores = new ArrayList<>();
        cores.add(ModItems.COMPRESSION_CORE.get());
        cores.add(ModItems.COMPRESSION_CORE_2.get());
        for (var deferred : ModItems.COMPRESSION_CORES_BY_BAND.values()) {
            cores.add(deferred.get());
        }
        return cores;
    }

    private static void registerInfo(IRecipeRegistration registration) {
        registration.addIngredientInfo(ModItems.COMPRESSED_COBBLESTONE.get(),
                Component.translatable("jei.rcc.info.compressed.line1"),
                Component.translatable("jei.rcc.info.compressed.line2"),
                Component.translatable("jei.rcc.info.compressed.line3"),
                Component.translatable("jei.rcc.info.compressed.line4"));
        registration.addIngredientInfo(ModItems.COMPRESSION_CORE.get(),
                Component.translatable("jei.rcc.info.core.line1"),
                Component.translatable("jei.rcc.info.core.line2"),
                Component.translatable("jei.rcc.info.core.line3"),
                Component.translatable("jei.rcc.info.core.line4"));
        registration.addIngredientInfo(ModItems.COMPRESSION_INTERFACE.get(),
                Component.translatable("jei.rcc.info.interface.line1"),
                Component.translatable("jei.rcc.info.interface.line2"));
        registration.addIngredientInfo(ModItems.COMPRESSION_PORT.get(),
                Component.translatable("jei.rcc.info.port.line1"),
                Component.translatable("jei.rcc.info.port.line2"),
                Component.translatable("jei.rcc.info.port.line3"),
                Component.translatable("jei.rcc.info.port.line4"));
    }

    private static List<CompressionDisplayRecipe> buildExamples() {
        List<CompressionDisplayRecipe> recipes = new ArrayList<>();
        recipes.add(compressExample(BigInteger.ONE));
        recipes.add(compressExample(BigInteger.TWO));
        recipes.add(compressExample(BigInteger.valueOf(3)));
        recipes.add(decompressExample(BigInteger.valueOf(3)));
        recipes.add(decompressExample(BigInteger.TWO));
        recipes.add(decompressExample(BigInteger.ONE));
        return recipes;
    }

    private static CompressionDisplayRecipe compressExample(BigInteger tier) {
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            inputs.add(levelStack(tier));
        }
        return new CompressionDisplayRecipe(inputs, levelStack(tier.add(BigInteger.ONE)));
    }

    private static CompressionDisplayRecipe decompressExample(BigInteger tier) {
        ItemStack output = tier.compareTo(BigInteger.ONE) <= 0
                ? new ItemStack(Items.COBBLESTONE)
                : levelStack(tier.subtract(BigInteger.ONE));
        output.setCount(9);
        return new CompressionDisplayRecipe(List.of(levelStack(tier)), output);
    }

    private static ItemStack levelStack(BigInteger tier) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), tier);
        return stack;
    }

    private static List<ManipulatorDisplayRecipe> buildManipulatorExamples() {
        List<ManipulatorDisplayRecipe> recipes = new ArrayList<>();
        for (CompressionBand band : new CompressionBand[]{CompressionBand.BAND_1, CompressionBand.BAND_2}) {
            BigInteger minSeed = band.minInputTier();
            recipes.add(manipulatorExample(minSeed));
            recipes.add(manipulatorExample(minSeed.multiply(BigInteger.TWO)));
        }
        return recipes;
    }

    private static ManipulatorDisplayRecipe manipulatorExample(BigInteger seedTier) {
        BigInteger outTier = seedTier.shiftLeft(1);
        return new ManipulatorDisplayRecipe(
                levelStack(seedTier),
                levelStack(outTier),
                CompressionConfig.ENERGY_PER_CRAFT,
                CompressionConfig.requiredMassLog(seedTier));
    }
}
