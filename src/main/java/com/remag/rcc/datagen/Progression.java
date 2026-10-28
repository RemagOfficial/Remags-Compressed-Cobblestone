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

final class Progression {
    private Progression() {
    }

    static ResourceLocation advancementId(GeneratorTier tier) {
        return ResourceLocation.fromNamespaceAndPath(RCC.MODID, "generator/" + tier.getSerializedName());
    }

    static ResourceLocation tabRootId() {
        return ResourceLocation.fromNamespaceAndPath(RCC.MODID, "generator/root");
    }

    static ResourceLocation recipeId(GeneratorTier tier) {
        return ResourceLocation.fromNamespaceAndPath(RCC.MODID, "cobblestone_generator_" + tier.getSerializedName());
    }

    static ResourceLocation rewardAdvancementId() {
        return ResourceLocation.fromNamespaceAndPath(RCC.MODID, "reward/compression_editor");
    }

    static Criterion<InventoryChangeTrigger.TriggerInstance> hasCompressedTier(BigInteger n) {
        ItemPredicate predicate = ItemPredicate.Builder.item()
                .of(ModItems.COMPRESSED_COBBLESTONE.get())
                .hasComponents(DataComponentPredicate.builder()
                        .expect(ModDataComponents.COMPRESSION_LEVEL.get(), n)
                        .build())
                .build();
        return InventoryChangeTrigger.TriggerInstance.hasItems(predicate);
    }

    static Criterion<InventoryChangeTrigger.TriggerInstance> hasCompressedTier(int n) {
        return hasCompressedTier(BigInteger.valueOf(n));
    }

    static Criterion<InventoryChangeTrigger.TriggerInstance> hasVanillaCobblestone() {
        return InventoryChangeTrigger.TriggerInstance.hasItems(Blocks.COBBLESTONE);
    }

    static Ingredient compressedTierIngredient(BigInteger n) {
        return DataComponentIngredient.of(false, ModDataComponents.COMPRESSION_LEVEL.get(),
                n, ModItems.COMPRESSED_COBBLESTONE.get());
    }

    static Ingredient compressedTierIngredient(int n) {
        return compressedTierIngredient(BigInteger.valueOf(n));
    }

    static ItemStack generatorStack(GeneratorTier tier) {
        ItemStack stack = new ItemStack(ModItems.COBBLESTONE_GENERATOR.get());
        stack.set(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(CobblestoneGeneratorBlock.TIER, tier));
        return stack;
    }

    static ItemStack compressedCobblestoneStack(BigInteger n) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), n);
        return stack;
    }

    static ItemStack compressedCobblestoneStack(int n) {
        return compressedCobblestoneStack(BigInteger.valueOf(n));
    }
}
