package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeTier;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
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
        buildMultiblockRecipes(pWriter);
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

    /**
     * The multiblock line: the shell every compression structure is built from, and the three cores that seal them.
     * Until now all seven of these were creative-only, which meant the shipped multiblocks could not actually be
     * <em>made</em> - and a band-2 tower would have inherited the same dead end. PLACEHOLDER shapes in the same spirit
     * as the machine recipes above; what matters is the shape of the dependency chain, not the costs.
     *
     * <p>Each layer only asks for the layer below it: frame and glass from raw materials, ports and the interface
     * from that shell, and every core from a shell plus one milestone block - the block the machine exists to make,
     * so the gate is the progression itself rather than an arbitrary pile of iron.
     */
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

        // A port is the shell's functional face: four frame blocks around one redstone block. Note that the frame and
        // glass ingredients are plain item matches on purpose - {@link DataComponentIngredient} compares whole
        // component values, so spelling out a {@code block_state} here would pin every recipe to one variant.
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

        // The milestone every band-1 core is gated on, read from the band table instead of written as a literal: a
        // band-2 core recipe will ask for BAND_2.rungStartExponent() and nothing else about this shape changes.
        BigInteger milestone = bandOneMilestone();
        Ingredient heart = Progression.compressedTierIngredient(milestone);
        Criterion<?> hasHeart = Progression.hasCompressedTier(milestone);

        ItemStack compressorCore = new ItemStack(ModItems.COMPRESSION_CORE.get());
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, compressorCore)
                .define('F', ModBlocks.FRAME.get())
                .define('P', ModItems.COMPRESSION_PORT.get())
                .define('H', heart)
                .pattern("FPF")
                .pattern("PHP")
                .pattern("FPF")
                .unlockedBy("has_milestone_cobblestone", hasHeart)
                .save(pWriter, rl("compression_core"));

        // The decompressor core mirrors it, with viewing glass where the compressor has structural frame - and it is
        // unlocked by owning the compressor core, because climbing back down is only interesting once you can climb up.
        ItemStack decompressorCore = new ItemStack(ModItems.COMPRESSION_DECOMPRESSOR_CORE.get());
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, decompressorCore)
                .define('G', ModBlocks.GLASS.get())
                .define('P', ModItems.COMPRESSION_PORT.get())
                .define('H', heart)
                .pattern("GPG")
                .pattern("PHP")
                .pattern("GPG")
                .unlockedBy("has_milestone_cobblestone", hasHeart)
                .unlockedBy("has_compression_core", has(ModItems.COMPRESSION_CORE.get()))
                .save(pWriter, rl("compression_decompressor_core"));

        // The tier-1 generator's core, mirroring the single-block generator ladder (power face above, milestone
        // blocks either side of a frame, a redstone drive below). The result is preset to rung I through its
        // {@code block_state} component. That is not decoration: {@link DataComponentIngredient} needs the component
        // to be present at all, so an unspecced rung-I core would be the only link the ladder below could not make.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, generatorCoreStack(GeneratorCoreTier.T1_P1000))
                .define('P', ModItems.COMPRESSION_PORT.get())
                .define('C', heart)
                .define('F', ModBlocks.FRAME.get())
                .define('R', Items.REDSTONE_BLOCK)
                .pattern(" P ")
                .pattern("CFC")
                .pattern(" R ")
                .unlockedBy("has_milestone_cobblestone", hasHeart)
                .save(pWriter, rl("generator_core"));

        // The rest of the BCE fountain ladder. Rungs II..X were never missing from the machine - the block entity has
        // always read its rate and FE price live off the RUNG state, and the creative tab lists all ten - they were
        // only missing from the craftable line. Each step spends the previous rung's core (so climbing costs the old
        // run, as the pipe ladder does) in the centre of four pieces of the milestone that rung mints, which keeps the
        // gate the progression itself instead of a pile of old cores: one rung per craft, so the cost cannot grow
        // exponentially the way the two-pipe ladder's would over ten steps. PLACEHOLDER counts as always. Each is id'd
        // {@code generator_core_<rung>}.
        GeneratorCoreTier[] rungs = GeneratorCoreTier.values();
        for (int i = 1; i < rungs.length; i++) {
            GeneratorCoreTier previous = rungs[i - 1];
            GeneratorCoreTier rung = rungs[i];
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, generatorCoreStack(rung))
                    .define('C', DataComponentIngredient.of(false, DataComponents.BLOCK_STATE,
                            rungState(previous), ModItems.GENERATOR_CORE.get()))
                    .define('H', Progression.compressedTierIngredient(rung.tierExponent()))
                    .pattern("H H")
                    .pattern(" C ")
                    .pattern("H H")
                    .unlockedBy("has_generator_core", has(ModItems.GENERATOR_CORE.get()))
                    .unlockedBy("has_rung_cobblestone", Progression.hasCompressedTier(rung.tierExponent()))
                    .save(pWriter, rl("generator_core_" + rung.getSerializedName()));
        }
    }

    /**
     * The compressed cobblestone tier every band-1 machine is gated on: the band's rung start, which by the table's
     * handover rule <em>is</em> the previous band's ceiling ({@code 1000}). Derived from {@link CompressionBand} so
     * the recipe line cannot drift from the numbers the machines themselves enforce when they check a seed's tier
     * ({@link com.remag.rcc.machine.CompressionCoreBlockEntity}'s min-in / max-out gates).
     */
    private static BigInteger bandOneMilestone() {
        return CompressionBand.BAND_1.rungStartExponent();
    }

    /** A recipe result/match stack for one pipe tier, carrying the {@code block_state} component like the tab entries. */
    private static ItemStack pipeStack(BcePipeTier tier) {
        ItemStack stack = new ItemStack(ModItems.BCE_PIPE.get());
        stack.set(DataComponents.BLOCK_STATE, pipeState(tier));
        return stack;
    }

    /**
     * A generator core stack preset to one rung, carrying {@code block_state} exactly like
     * {@link com.remag.rcc.block.ModCreativeModeTab}'s entries - which is what lets the ladder's ingredient match it
     * and what makes the item's name and tooltip read {@code "Generator Core (VII)"}.
     */
    private static ItemStack generatorCoreStack(GeneratorCoreTier rung) {
        ItemStack stack = new ItemStack(ModItems.GENERATOR_CORE.get());
        stack.set(DataComponents.BLOCK_STATE, rungState(rung));
        return stack;
    }

    /**
     * The state selection for one rung, shared by {@link #generatorCoreStack} and the ladder's matching ingredient.
     * Only the rung: {@code RUNG} is the block's whole state definition, so a crafted stack, a broken one and a
     * creative-tab one all serialise to the same value, which an exact component match requires.
     */
    private static BlockItemStateProperties rungState(GeneratorCoreTier rung) {
        return BlockItemStateProperties.EMPTY.with(GeneratorCoreBlock.RUNG, rung);
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
