package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.CobblestoneGeneratorBlock;
import com.remag.rcc.machine.GeneratorTier;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.math.BigInteger;

/**
 * Shared glue for the cobblestone-generator progression line so the two datagen providers that build it -
 * {@link ModAdvancementProvider} (the "giant straight line" of advancements) and {@link ModRecipeProvider} (the gated
 * crafting recipes) - cannot drift apart. Both ask for the <em>same</em> thing: one rung is unlocked by obtaining the
 * compressed cobblestone whose tier is {@link GeneratorTier#gateCobblestoneTier()} - the block worth exactly one second
 * of that generator's output - so the advancement trigger, the recipe's centre ingredient, and the recipe's unlock all
 * reference one number and one predicate.
 */
final class Progression {
    private Progression() {
    }

    /** The advancement id for one rung's gate; the ladder is a straight line keyed off {@link GeneratorTier#BASE}. */
    static ResourceLocation advancementId(GeneratorTier tier) {
        return ResourceLocation.fromNamespaceAndPath(RCC.MODID, "generator/" + tier.getSerializedName());
    }

    /** The id of the structural tab root that names the advancement tab and starts the whole line. */
    static ResourceLocation tabRootId() {
        return ResourceLocation.fromNamespaceAndPath(RCC.MODID, "generator/root");
    }

    /** The recipe id for one rung's generator (mirrors the advancement naming, under {@code generator/}). */
    static ResourceLocation recipeId(GeneratorTier tier) {
        return ResourceLocation.fromNamespaceAndPath(RCC.MODID, "cobblestone_generator_" + tier.getSerializedName());
    }

    /**
     * The "have a tier-{@code n} compressed cobblestone in your inventory" criterion. Matches the single shared
     * {@link ModItems#COMPRESSED_COBBLESTONE} item filtered on its {@code compression_level} component equal to
     * {@code n} - the only way to tell one tier from another, since every level is the same item. Non-strict is not a
     * concern here: a component predicate always compares whole values.
     */
    static Criterion<InventoryChangeTrigger.TriggerInstance> hasCompressedTier(int n) {
        ItemPredicate predicate = ItemPredicate.Builder.item()
                .of(ModItems.COMPRESSED_COBBLESTONE.get())
                .hasComponents(DataComponentPredicate.builder()
                        .expect(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.valueOf(n))
                        .build())
                .build();
        return InventoryChangeTrigger.TriggerInstance.hasItems(predicate);
    }

    /**
     * The "pick up a piece of vanilla cobblestone" criterion - the tab root's trigger. It is the mod's literal
     * starting point (the whole ladder is one giant game of "compress the cobblestone") and auto-completes for every
     * player, so the tab opens without a contrived impossible gate.
     */
    static Criterion<InventoryChangeTrigger.TriggerInstance> hasVanillaCobblestone() {
        return InventoryChangeTrigger.TriggerInstance.hasItems(Blocks.COBBLESTONE);
    }

    /**
     * The tier-{@code n} compressed cobblestone as a recipe ingredient (the gate block spent to build a generator).
     * Matched on the {@code compression_level} component so a lower tier cannot stand in for the milestone the rung
     * demands - the same exact-value match {@link ModRecipeProvider}'s pipe ladder uses for {@code block_state}.
     */
    static Ingredient compressedTierIngredient(int n) {
        return DataComponentIngredient.of(false, ModDataComponents.COMPRESSION_LEVEL.get(),
                BigInteger.valueOf(n), ModItems.COMPRESSED_COBBLESTONE.get());
    }

    /** A generator stack preset to one rung through its {@code block_state} component - the recipe result / icon. */
    static ItemStack generatorStack(GeneratorTier tier) {
        ItemStack stack = new ItemStack(ModItems.COBBLESTONE_GENERATOR.get());
        stack.set(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(CobblestoneGeneratorBlock.TIER, tier));
        return stack;
    }

    /**
     * A tier-{@code n} compressed cobblestone stack carrying the {@code compression_level} data component. Used as the
     * advancement icon so the visual matches what the criterion actually asks for.
     */
    static ItemStack compressedCobblestoneStack(int n) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.valueOf(n));
        return stack;
    }
}
