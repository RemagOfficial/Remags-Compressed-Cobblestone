package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeTier;
import com.remag.rcc.machine.GeneratorTier;
import com.remag.rcc.recipe.CompressRecipe;
import com.remag.rcc.recipe.DecompressRecipe;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.math.BigInteger;
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
        // Level 1 is the only data-driven recipe: 3x3 vanilla cobblestone -> compressed(level 1).
        // All higher compressions/decompressions are handled by the two dynamic recipes in Phase 3,
        // so the recipe count no longer scales with MAX_LEVELS.
        ItemStack levelOne = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        levelOne.set(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, levelOne)
                .define('#', Blocks.COBBLESTONE)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .unlockedBy("has_cobblestone", has(Blocks.COBBLESTONE))
                .save(pWriter, rl("compressed_cobblestone_one"));

        // The two dynamic recipes are tier-agnostic (they read the level off the grid), so each is a
        // single JSON regardless of MAX_LEVELS. No unlock advancement is attached.
        pWriter.accept(rl("compression"), new CompressRecipe(), null);
        pWriter.accept(rl("decompression"), new DecompressRecipe(), null);

        // The Auto-Compressor: a PLACEHOLDER shaped recipe using only early-game materials (no compressed tier is
        // required, so it can be crafted to bootstrap the climb). Tune the ingredients to taste.
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

        // The Auto Decompressor: PLACEHOLDER recipe mirroring the compressor's (the mirrored pattern hints at the
        // reverse direction). Tune the ingredients to taste.
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

        buildBceLogisticsRecipes(pWriter);
        buildGeneratorRecipes(pWriter);
    }

    /**
     * Cobblestone generators - one placeholder recipe per rung of the {@link GeneratorTier} ladder, each asking for
     * the same milestone block the rung's advancement asks for. The recipe consumes a tier-
     * {@link GeneratorTier#gateCobblestoneTier()} compressed cobblestone in its centre (matched by
     * {@code compression_level}, so a lower tier cannot stand in) surrounded by iron/frame/redstone - deliberately
     * light so the ladder's cost is the milestone itself, not the shell. Tune the pattern to taste; the important
     * part is the {@code unlockedBy} criterion, which mirrors the advancement so the recipe stays permanently
     * unlocked once the cobblestone has been obtained.
     */
    private void buildGeneratorRecipes(RecipeOutput pWriter) {
        for (GeneratorTier tier : GeneratorTier.values()) {
            int gate = tier.gateCobblestoneTier();
            ItemStack out = Progression.generatorStack(tier);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, out)
                    .define('I', Items.IRON_INGOT)
                    .define('C', Progression.compressedTierIngredient(gate))
                    .define('F', ModBlocks.FRAME.get())
                    .define('R', Items.REDSTONE)
                    .pattern(" I ")
                    .pattern("CFC")
                    .pattern(" R ")
                    .unlockedBy("has_gate_cobblestone", Progression.hasCompressedTier(gate))
                    .save(pWriter, Progression.recipeId(tier));
        }
    }

    /**
     * BCE logistics. PLACEHOLDER recipes in the same spirit as the two machine recipes above (early-game materials,
     * no compressed tier required).
     *
     * <p>Tier 1 is crafted from raw materials; tier 2 is a ladder step - three pipes of the previous tier plus a
     * redstone block, so improving throughput costs the old run. There is one recipe per tier and no more: which way a
     * pipe points is set in world with the wrench, not bought here. Each is id'd {@code bce_pipe_<tier>}.
     */
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

        // The upgrade ladder: 3 previous-tier pipes + 1 redstone block -> 1 pipe of the next tier.
        for (BcePipeTier tier : BcePipeTier.values()) {
            if (tier == BcePipeTier.TIER_1) {
                continue; // crafted from raw materials above
            }
            BcePipeTier previous = BcePipeTier.values()[tier.ordinal() - 1];
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, pipeStack(tier))
                    // The previous variant must be matched on its block_state component, or a plain Ingredient
                    // (whose codec serialises through SIMPLE_ITEM_CODEC and compares the item only) would let any
                    // pipe stand in for it and skip the whole ladder. Non-strict, so a renamed pipe still upgrades.
                    // This exact-value match is also why a broken pipe drops its tier and not its direction: see
                    // BcePipeBlockEntity#collectImplicitComponents.
                    .define('P', DataComponentIngredient.of(false, DataComponents.BLOCK_STATE,
                            pipeState(previous), ModItems.BCE_PIPE.get()))
                    .define('R', Items.REDSTONE_BLOCK)
                    .pattern("PP")
                    .pattern("PR")
                    .unlockedBy("has_pipe", has(ModItems.BCE_PIPE.get()))
                    .save(pWriter, rl("bce_pipe_" + tier.getSerializedName()));
        }

        // The tank: a frame shell around one interface panel (the machine that reads BCE is what makes the shell hold it).
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, new ItemStack(ModItems.BCE_STORAGE.get()))
                .define('F', ModBlocks.FRAME.get())
                .define('I', ModBlocks.INTERFACE.get())
                .pattern("F F")
                .pattern("FIF")
                .pattern("FFF")
                .unlockedBy("has_interface", has(ModItems.COMPRESSION_INTERFACE.get()))
                .save(pWriter, rl("bce_storage"));

        // The tank ladder, shaped like the pipe ladder above: a tier-I tank plus a frame shell. No component matching
        // is needed this time - the two tank tiers are separate items, so a plain ingredient already pins the rung.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, new ItemStack(ModItems.BCE_STORAGE_2.get()))
                .define('F', ModBlocks.FRAME.get())
                .define('T', ModItems.BCE_STORAGE.get())
                .pattern("FF")
                .pattern("FT")
                .unlockedBy("has_storage", has(ModItems.BCE_STORAGE.get()))
                .save(pWriter, rl("bce_storage_2"));

        // The wrench: iron heads on a stick. What makes it useful is not the shape but the tag - ModItemTagGenerator
        // publishes it onto c:tools/wrench, so it configures other mods' machines and theirs configure ours.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, new ItemStack(ModItems.WRENCH.get()))
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .pattern(" II")
                .pattern(" I ")
                .pattern("S  ")
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(pWriter, rl("wrench"));
    }

    /** A recipe result/match stack for one pipe tier, carrying the {@code block_state} component like the tab entries. */
    private static ItemStack pipeStack(BcePipeTier tier) {
        ItemStack stack = new ItemStack(ModItems.BCE_PIPE.get());
        stack.set(DataComponents.BLOCK_STATE, pipeState(tier));
        return stack;
    }

    /**
     * The state selection for one pipe tier, shared by the stack builder and the ladder's matching ingredient. Only
     * the tier: the direction is an in-world configuration, and {@link net.neoforged.neoforge.common.crafting.DataComponentIngredient}
     * compares whole component values, so a mode here would have to be spelled out in every recipe too.
     */
    private static BlockItemStateProperties pipeState(BcePipeTier tier) {
        return BlockItemStateProperties.EMPTY.with(BcePipeBlock.TIER, tier);
    }
}
