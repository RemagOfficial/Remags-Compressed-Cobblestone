package com.remag.rcc.client;

import com.remag.rcc.item.BcePipeItem;
import com.remag.rcc.item.CobblestoneGeneratorItem;
import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeTier;
import com.remag.rcc.machine.CobblestoneGeneratorBlock;
import com.remag.rcc.machine.GeneratorCoreBlock;
import com.remag.rcc.machine.GeneratorTier;
import com.remag.rcc.registry.Banded;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the tier (top-left) and level (top-right) numbers stamped over machine/part item icons in GUIs.
 * Reads the same BLOCK_STATE data components the items already use for tinting, so a badge always matches
 * the icon the player is looking at. Returns {@code null} for items that carry neither a tier nor a level
 * (ports, auto machines, plain cobblestone), so they stay clean.
 */
public final class MachineItemBadge {

    private MachineItemBadge() {
    }

    // level is nullable: only items with two independent axes (generator cores) show a level; the rest are tier-only.
    public record Badge(int tier, Integer level) {
    }

    @Nullable
    public static Badge of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        BlockItemStateProperties props = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);

        // Single-block generator is a tier-0 machine that produces a cobblestone ladder rung: tier 0, level = rung index.
        if (stack.getItem() instanceof CobblestoneGeneratorItem) {
            GeneratorTier tier = props.get(CobblestoneGeneratorBlock.TIER);
            if (tier == null) {
                tier = GeneratorTier.BASE;
            }
            return new Badge(0, tier.ordinal());
        }

        // Pipes carry only a tier (no separate level).
        if (stack.getItem() instanceof BcePipeItem) {
            BcePipeTier tier = props.get(BcePipeBlock.TIER);
            if (tier == null) {
                tier = BcePipeTier.TIER_1;
            }
            return new Badge(tier.number(), null);
        }

        // Banded cores: band is the tier. Generator cores additionally expose a rung as their level.
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof Banded banded) {
            int tier = banded.band().index();
            Integer level = null;
            if (blockItem.getBlock() instanceof GeneratorCoreBlock) {
                Integer rung = props.get(GeneratorCoreBlock.RUNG);
                level = rung == null ? 0 : rung;
            }
            return new Badge(tier, level);
        }

        return null;
    }
}
