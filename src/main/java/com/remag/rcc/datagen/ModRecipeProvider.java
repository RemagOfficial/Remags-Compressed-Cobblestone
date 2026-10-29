package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeTier;
import com.remag.rcc.machine.CobblestoneGeneratorBlock;
import com.remag.rcc.machine.GeneratorCoreBlock;
import com.remag.rcc.machine.GeneratorCoreTier;
import com.remag.rcc.machine.GeneratorTier;
import com.remag.rcc.recipe.CompressRecipe;
import com.remag.rcc.recipe.DecompressRecipe;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ModRecipeProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(pOutput, lookupProvider);
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RCC.MODID, path);
    }

    @Override
    protected void buildRecipes(RecipeOutput pWriter) {
        ItemStack levelOne = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        levelOne.set(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, levelOne)
                .define('#', Blocks.COBBLESTONE)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .unlockedBy("has_cobblestone", has(Blocks.COBBLESTONE))
                .save(pWriter, rl("compressed_cobblestone_one"));

        pWriter.accept(rl("compression"), new CompressRecipe(), null);
        pWriter.accept(rl("decompression"), new DecompressRecipe(), null);

        ItemStack autoBlock = new ItemStack(ModItems.COMPRESSION_AUTO.get());
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, autoBlock)
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('F', Blocks.FURNACE)
                .define('G', Blocks.GLASS)
                .pattern("IGI")
                .pattern("RFR")
                .pattern("III")
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(pWriter, rl("compression_auto"));

        ItemStack decompressorBlock = new ItemStack(ModItems.COMPRESSION_AUTO_DECOMPRESSOR.get());
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, decompressorBlock)
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('F', Blocks.FURNACE)
                .define('G', Blocks.GLASS)
                .pattern("III")
                .pattern("RFR")
                .pattern("IGI")
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(pWriter, rl("compression_auto_decompressor"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, new ItemStack(ModItems.COMPRESSION_EDITOR.get()))
                .define('G', ModBlocks.GLASS.get())
                .define('I', ModBlocks.INTERFACE.get())
                .define('C', Progression.compressedTierIngredient(CompressionBand.TOP.maxOutputTier()))
                .pattern("GIG")
                .pattern("ICI")
                .pattern("GIG")
                .unlockedBy("has_googol_cobblestone",
                        Progression.hasCompressedTier(CompressionBand.TOP.maxOutputTier()))
                .save(pWriter, rl("compression_editor"));

        buildBceLogisticsRecipes(pWriter);
        buildGeneratorRecipes(pWriter);
        buildMultiblockRecipes(pWriter);
    }

    
    private void buildGeneratorRecipes(RecipeOutput pWriter) {
        GeneratorTier[] all = GeneratorTier.values();
        for (GeneratorTier tier : all) {
            int gate = tier.gateCobblestoneTier();
            ItemStack out = Progression.generatorStack(tier);
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, out)
                    .define('I', Items.IRON_INGOT)
                    .define('C', Progression.compressedTierIngredient(gate))
                    .define('R', Items.REDSTONE)
                    .pattern(" I ")
                    .pattern("CFC")
                    .pattern(" R ");
            // Base is built from a frame; each higher rung is crafted by upgrading the generator below it,
            // matched on its block_state tier so only the exact previous rung is accepted.
            if (tier == GeneratorTier.BASE) {
                builder.define('F', ModBlocks.FRAME.get());
            } else {
                builder.define('F', DataComponentIngredient.of(false, DataComponents.BLOCK_STATE,
                        generatorState(all[tier.ordinal() - 1]), ModItems.COBBLESTONE_GENERATOR.get()));
            }
            builder.unlockedBy("has_gate_cobblestone", Progression.hasCompressedTier(gate))
                    .save(pWriter, Progression.recipeId(tier));
        }
    }

    
    private void buildBceLogisticsRecipes(RecipeOutput pWriter) {
        ItemStack tierOne = pipeStack(BcePipeTier.TIER_1);
        tierOne.setCount(4);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tierOne)
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('F', ModBlocks.FRAME.get())
                .pattern("RIR")
                .pattern("IFI")
                .pattern("IRI")
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(pWriter, rl("bce_pipe_one"));

        for (BcePipeTier tier : BcePipeTier.values()) {
            if (tier == BcePipeTier.TIER_1) {
                continue;
            }
            BcePipeTier previous = BcePipeTier.values()[tier.ordinal() - 1];
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, pipeStack(tier))
                    .define('P', DataComponentIngredient.of(false, DataComponents.BLOCK_STATE,
                            pipeState(previous), ModItems.BCE_PIPE.get()))
                    .define('R', Items.REDSTONE_BLOCK)
                    .pattern("PP")
                    .pattern("PR")
                    .unlockedBy("has_pipe", has(ModItems.BCE_PIPE.get()))
                    .save(pWriter, rl("bce_pipe_" + tier.getSerializedName()));
        }

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, new ItemStack(ModItems.BCE_STORAGE.get()))
                .define('F', ModBlocks.FRAME.get())
                .define('I', ModBlocks.INTERFACE.get())
                .pattern("F F")
                .pattern("FIF")
                .pattern("FFF")
                .unlockedBy("has_interface", has(ModItems.COMPRESSION_INTERFACE.get()))
                .save(pWriter, rl("bce_storage"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, new ItemStack(ModItems.BCE_STORAGE_2.get()))
                .define('F', ModBlocks.FRAME.get())
                .define('T', ModItems.BCE_STORAGE.get())
                .pattern("FF")
                .pattern("FT")
                .unlockedBy("has_storage", has(ModItems.BCE_STORAGE.get()))
                .save(pWriter, rl("bce_storage_2"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, new ItemStack(ModItems.BCE_STORAGE_3.get()))
                .define('F', ModBlocks.FRAME.get())
                .define('T', ModItems.BCE_STORAGE_2.get())
                .pattern("FF")
                .pattern("FT")
                .unlockedBy("has_storage", has(ModItems.BCE_STORAGE_2.get()))
                .save(pWriter, rl("bce_storage_3"));

        for (CompressionBand band : CompressionBand.values()) {
            if (band.index() < 3) {
                continue;
            }
            Item tank = ModItems.BCE_STORAGES_BY_BAND.get(band).get();
            Item previous = storageItemForBand(band.index() - 1);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, new ItemStack(tank))
                    .define('F', ModBlocks.FRAME.get())
                    .define('T', previous)
                    .pattern("FF")
                    .pattern("FT")
                    .unlockedBy("has_storage", has(previous))
                    .save(pWriter, rl("bce_storage_" + (band.index() + 1)));
        }

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, new ItemStack(ModItems.WRENCH.get()))
                .define('I', Items.IRON_INGOT)
                .define('B', Items.IRON_BLOCK)
                .pattern(" I ")
                .pattern(" BI")
                .pattern("I  ")
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(pWriter, rl("wrench"));
    }

    
    private void buildMultiblockRecipes(RecipeOutput pWriter) {
        ItemStack frame = new ItemStack(ModItems.COMPRESSION_FRAME.get());
        frame.setCount(4);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, frame)
                .define('I', Items.IRON_INGOT)
                .define('C', Blocks.COBBLESTONE)
                .pattern("I I")
                .pattern(" C ")
                .pattern("I I")
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(pWriter, rl("compression_frame"));

        ItemStack glass = new ItemStack(ModItems.COMPRESSION_GLASS.get());
        glass.setCount(4);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, glass)
                .define('G', Blocks.GLASS)
                .define('F', ModBlocks.FRAME.get())
                .pattern("GG")
                .pattern("GF")
                .unlockedBy("has_frame", has(ModItems.COMPRESSION_FRAME.get()))
                .save(pWriter, rl("compression_glass"));

        ItemStack port = new ItemStack(ModItems.COMPRESSION_PORT.get());
        port.setCount(4);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, port)
                .define('F', ModBlocks.FRAME.get())
                .define('R', Items.REDSTONE_BLOCK)
                .pattern("F F")
                .pattern(" R ")
                .pattern("F F")
                .unlockedBy("has_frame", has(ModItems.COMPRESSION_FRAME.get()))
                .save(pWriter, rl("compression_port"));

        ItemStack interfacePanel = new ItemStack(ModItems.COMPRESSION_INTERFACE.get());
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, interfacePanel)
                .define('G', ModBlocks.GLASS.get())
                .define('F', ModBlocks.FRAME.get())
                .pattern(" G ")
                .pattern("GFG")
                .pattern(" G ")
                .unlockedBy("has_glass", has(ModItems.COMPRESSION_GLASS.get()))
                .save(pWriter, rl("compression_interface"));

        buildCoreRecipes(pWriter, CompressionBand.BAND_1, ModItems.COMPRESSION_CORE.get(),
                ModItems.COMPRESSION_DECOMPRESSOR_CORE.get(), ModItems.GENERATOR_CORE.get(),
                "compression_core", "compression_decompressor_core", "generator_core");
        buildCoreRecipes(pWriter, CompressionBand.BAND_2, ModItems.COMPRESSION_CORE_2.get(),
                ModItems.COMPRESSION_DECOMPRESSOR_CORE_2.get(), ModItems.GENERATOR_CORE_2.get(),
                "compression_core_2", "compression_decompressor_core_2", "generator_core_2");
        for (CompressionBand band : CompressionBand.values()) {
            if (band.index() < 3) {
                continue;
            }
            int n = band.index();
            Item decompressor = band.hasDecompressor() ? ModItems.COMPRESSION_DECOMPRESSOR_CORE_3.get() : null;
            buildCoreRecipes(pWriter, band,
                    ModItems.COMPRESSION_CORES_BY_BAND.get(band).get(),
                    decompressor,
                    ModItems.GENERATOR_CORES_BY_BAND.get(band).get(),
                    "compression_core_" + n,
                    n == 3 ? "compression_decompressor_core_3" : null,
                    "generator_core_" + n);
        }
    }

    
    private void buildCoreRecipes(RecipeOutput pWriter, CompressionBand band, Item compressorItem,
                                  Item decompressorItem, Item generatorCoreItem, String compressorId,
                                  String decompressorId, String generatorCoreId) {
        BigInteger milestone = band.rungStartExponent();
        Ingredient heart = Progression.compressedTierIngredient(milestone);
        Criterion<?> hasHeart = Progression.hasCompressedTier(milestone);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, new ItemStack(compressorItem))
                .define('F', ModBlocks.FRAME.get())
                .define('P', ModItems.COMPRESSION_PORT.get())
                .define('H', heart)
                .pattern("FPF")
                .pattern("PHP")
                .pattern("FPF")
                .unlockedBy("has_milestone_cobblestone", hasHeart)
                .save(pWriter, rl(compressorId));

        if (decompressorItem != null) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, new ItemStack(decompressorItem))
                    .define('G', ModBlocks.GLASS.get())
                    .define('P', ModItems.COMPRESSION_PORT.get())
                    .define('H', heart)
                    .pattern("GPG")
                    .pattern("PHP")
                    .pattern("GPG")
                    .unlockedBy("has_milestone_cobblestone", hasHeart)
                    .unlockedBy("has_compression_core", has(compressorItem))
                    .save(pWriter, rl(decompressorId));
        }

        List<GeneratorCoreTier> rungs = GeneratorCoreTier.rungsFor(band);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, generatorCoreStack(generatorCoreItem, rungs.get(0)))
                .define('P', ModItems.COMPRESSION_PORT.get())
                .define('C', heart)
                .define('F', ModBlocks.FRAME.get())
                .define('R', Items.REDSTONE_BLOCK)
                .pattern(" P ")
                .pattern("CFC")
                .pattern(" R ")
                .unlockedBy("has_milestone_cobblestone", hasHeart)
                .save(pWriter, rl(generatorCoreId));

        for (int i = 1; i < rungs.size(); i++) {
            GeneratorCoreTier previous = rungs.get(i - 1);
            GeneratorCoreTier rung = rungs.get(i);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, generatorCoreStack(generatorCoreItem, rung))
                    .define('C', DataComponentIngredient.of(false, DataComponents.BLOCK_STATE,
                            rungState(previous), generatorCoreItem))
                    .define('H', Progression.compressedTierIngredient(rung.tierExponent()))
                    .pattern("H H")
                    .pattern(" C ")
                    .pattern("H H")
                    .unlockedBy("has_generator_core", has(generatorCoreItem))
                    .unlockedBy("has_rung_cobblestone", Progression.hasCompressedTier(rung.tierExponent()))
                    .save(pWriter, rl("generator_core_" + rung.getSerializedName()));
        }
    }

    
    private static Item storageItemForBand(int bandIndex) {
        return switch (bandIndex) {
            case 0 -> ModItems.BCE_STORAGE.get();
            case 1 -> ModItems.BCE_STORAGE_2.get();
            case 2 -> ModItems.BCE_STORAGE_3.get();
            default -> ModItems.BCE_STORAGES_BY_BAND.get(CompressionBand.values()[bandIndex]).get();
        };
    }

    
    private static ItemStack pipeStack(BcePipeTier tier) {
        ItemStack stack = new ItemStack(ModItems.BCE_PIPE.get());
        stack.set(DataComponents.BLOCK_STATE, pipeState(tier));
        return stack;
    }

    
    private static ItemStack generatorCoreStack(Item coreItem, GeneratorCoreTier rung) {
        ItemStack stack = new ItemStack(coreItem);
        stack.set(DataComponents.BLOCK_STATE, rungState(rung));
        return stack;
    }

    
    private static BlockItemStateProperties rungState(GeneratorCoreTier rung) {
        return BlockItemStateProperties.EMPTY.with(GeneratorCoreBlock.RUNG, rung.index());
    }

    
    private static BlockItemStateProperties pipeState(BcePipeTier tier) {
        return BlockItemStateProperties.EMPTY.with(BcePipeBlock.TIER, tier);
    }

    private static BlockItemStateProperties generatorState(GeneratorTier tier) {
        return BlockItemStateProperties.EMPTY.with(CobblestoneGeneratorBlock.TIER, tier);
    }
}
