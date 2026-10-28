package com.remag.rcc.datagen;

import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.GeneratorTier;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.DisplayNames;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModAdvancementProvider extends net.neoforged.neoforge.common.data.AdvancementProvider {
    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                  ExistingFileHelper existingFileHelper) {
        super(output, registries, existingFileHelper, List.of(new GeneratorLine()));
    }

    private static final class GeneratorLine implements AdvancementGenerator {
        @Override
        public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver,
                             ExistingFileHelper existingFileHelper) {
            AdvancementHolder parent = Advancement.Builder.advancement()
                    .display(new ItemStack(Items.COBBLESTONE),
                            Component.translatable("advancement.rcc.generator.root"),
                            Component.translatable("advancement.rcc.generator.root.description"),
                            TAB_BACKGROUND, AdvancementType.TASK, false, false, false)
                    .addCriterion("cobblestone", Progression.hasVanillaCobblestone())
                    .save(saver, Progression.tabRootId(), existingFileHelper);

            for (GeneratorTier tier : GeneratorTier.values()) {
                int gate = tier.gateCobblestoneTier();
                Component title = Component.translatable("advancement.rcc." + tier.getSerializedName());
                Component description =
                        Component.translatable("advancement.rcc." + tier.getSerializedName() + ".description");

                Advancement.Builder builder = Advancement.Builder.advancement()
                        .parent(parent)
                        .display(Progression.compressedCobblestoneStack(gate), title, description,
                                null, AdvancementType.TASK, true, true, false)
                        .addCriterion("cobblestone", Progression.hasCompressedTier(gate));
                parent = builder.save(saver, Progression.advancementId(tier), existingFileHelper);
            }

            Advancement.Builder.advancement()
                    .parent(parent)
                    .display(new ItemStack(ModItems.COMPRESSION_EDITOR.get()),
                            Component.translatable("advancement.rcc.reward.compression_editor"),
                            Component.translatable("advancement.rcc.reward.compression_editor.description"),
                            null, AdvancementType.GOAL, true, true, true)
                    .addCriterion("has_googol_cobblestone",
                            Progression.hasCompressedTier(CompressionBand.TOP.maxOutputTier()))
                    .save(saver, Progression.rewardAdvancementId(), existingFileHelper);
        }
    }

    private static final ResourceLocation TAB_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/block/cobblestone.png");

    static String titleFor(GeneratorTier tier) {
        if (tier == GeneratorTier.BASE) {
            return "Only the Beginning";
        }
        return DisplayNames.compressed(BigInteger.valueOf(tier.gateCobblestoneTier()));
    }

    static String descriptionFor(GeneratorTier tier) {
        if (tier == GeneratorTier.BASE) {
            return "Reach a " + DisplayNames.compressed(BigInteger.valueOf(tier.gateCobblestoneTier()))
                    + " to craft your first Cobblestone Generator.";
        }
        return "Reach " + DisplayNames.compressed(BigInteger.valueOf(tier.gateCobblestoneTier()))
                + " to unlock the Cobblestone Generator (" + tier.displaySuffix() + ").";
    }
}
