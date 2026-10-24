package com.remag.rcc.datagen;

import com.remag.rcc.machine.GeneratorTier;
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

/**
 * The cobblestone-generator progression backbone: one long, straight line of advancements - the "giant straight line"
 * that, once the mod is complete, spells out every compression tier needed to finish it. There is exactly one
 * advancement per {@link GeneratorTier} rung, each chained to the previous, and each unlocked by obtaining the
 * compressed cobblestone whose tier is {@link GeneratorTier#gateCobblestoneTier()} - the block worth one second of that
 * generator's output. Crafting the generator it gates is then unlocked by the matching recipe
 * ({@link ModRecipeProvider}), so the advancement line and the gated recipes describe one and the same climb.
 *
 * <p>The chain runs in {@link GeneratorTier} declaration order, starting at {@link GeneratorTier#BASE}. Its
 * advancement is the line's root - the only one carrying a background texture, which is what makes it open its own tab
 * in the advancements screen (1.21.1 groups tabs client-side per root). Because every rung after BASE is gated by a
 * strictly ascending tier, a player who completes the line has, by definition, compressed their way to the top
 * milestone the generators reach.
 */
public class ModAdvancementProvider extends net.neoforged.neoforge.common.data.AdvancementProvider {
    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                  ExistingFileHelper existingFileHelper) {
        super(output, registries, existingFileHelper, List.of(new GeneratorLine()));
    }

    /** Builds the whole straight line in one pass, keeping the last holder as the next advancement's parent. */
    private static final class GeneratorLine implements AdvancementGenerator {
        @Override
        public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver,
                             ExistingFileHelper existingFileHelper) {
            // The structural tab root: its title is what the advancements screen shows as the tab name, and its
            // background texture is what makes it open a tab at all. It is unlocked by the mod's literal starting
            // point - a piece of vanilla cobblestone - so it auto-completes and never blocks the line below it.
            AdvancementHolder parent = Advancement.Builder.advancement()
                    .display(new ItemStack(Items.COBBLESTONE),
                            Component.translatable("advancement.rcc.generator.root"),
                            Component.translatable("advancement.rcc.generator.root.description"),
                            TAB_BACKGROUND, AdvancementType.TASK, false, false, false)
                    .addCriterion("cobblestone", Progression.hasVanillaCobblestone())
                    .save(saver, Progression.tabRootId(), existingFileHelper);

            // The ladder proper: one advancement per rung, each a child of the last, icon = the milestone cobblestone
            // the criterion asks for (so the picture is the goal, not the reward).
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
                // save() returns the built holder and tracks it, so the next rung's parent check passes.
                parent = builder.save(saver, Progression.advancementId(tier), existingFileHelper);
            }
        }
    }

    /** A vanilla texture that always resolves, standing in for the mod's own tab background. */
    private static final ResourceLocation TAB_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/block/cobblestone.png");

    /**
     * The advancement title for one rung: BASE is the hand-named {@code "Only the Beginning"}; every other rung reads
     * as the milestone block it asks for. Used by {@link ModLanguageProvider} so the datagen name and the lang entry
     * share one definition.
     */
    static String titleFor(GeneratorTier tier) {
        if (tier == GeneratorTier.BASE) {
            return "Only the Beginning";
        }
        return DisplayNames.compressed(BigInteger.valueOf(tier.gateCobblestoneTier()));
    }

    /** The advancement description for one rung: the generator the milestone unlocks, spelled out from the rung. */
    static String descriptionFor(GeneratorTier tier) {
        if (tier == GeneratorTier.BASE) {
            return "Reach a " + DisplayNames.compressed(BigInteger.valueOf(tier.gateCobblestoneTier()))
                    + " to craft your first Cobblestone Generator.";
        }
        return "Reach " + DisplayNames.compressed(BigInteger.valueOf(tier.gateCobblestoneTier()))
                + " to unlock the Cobblestone Generator (" + tier.displaySuffix() + ").";
    }
}
