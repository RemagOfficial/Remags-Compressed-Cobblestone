package com.remag.rcc.item;

import com.remag.rcc.machine.CobblestoneGeneratorBlock;
import com.remag.rcc.machine.GeneratorTier;
import com.remag.rcc.registry.BceNotation;
import com.remag.rcc.registry.TowerTooltipComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

/**
 * The single item behind every cobblestone-generator rung. The rung rides on the stack's vanilla {@code block_state}
 * component (the same route {@link BcePipeItem} and {@link com.remag.rcc.machine.CompressionPortBlock} use), so one
 * registration covers the whole ladder while each creative entry still reads distinctly:
 * {@code "Cobblestone Generator (BCE Tier 500)"}, {@code "... (double-tuple)"}, {@code "... (Base)"}.
 *
 * <p>The hover text spells out what a rung does - its per-second rate and whether it pushes cobblestone items
 * (hopper/pipe-extractable) or raw BCE (extract-pipe-drainable) - because the names alone cannot convey the item/BCE
 * handover that defines the ladder. The rate is the one line a BCE rung cannot say in plain text ({@code 9^500} needs
 * a raised exponent, and {@code 9^(10^12)} needs a raised <em>stack</em>), so those rungs draw it instead - see
 * {@link #getTooltipImage(ItemStack)}.
 */
public class CobblestoneGeneratorItem extends BlockItem {

    public CobblestoneGeneratorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        // "Cobblestone Generator (BCE Tier 500)"; the suffix is composed (not a raw lang value) so it survives the
        // leading-whitespace-stripping of .properties files, mirroring BcePipeItem's tier suffix.
        return Component.translatable("block.rcc.cobblestone_generator")
                .append(Component.literal(" (" + tierOf(stack).displaySuffix() + ")"));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        GeneratorTier tier = tierOf(stack);
        // Item rungs have a whole cobblestone count to state and no exponent to raise, so they keep the text line;
        // a BCE rung's rate is drawn by getTooltipImage, which would otherwise print the same figure twice.
        if (tier.rateTower() == null) {
            tooltip.add(Component.translatable("tooltip.rcc.generator.rate", tier.describe())
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(
                        tier.outputIsItems() ? "tooltip.rcc.generator.hint.items" : "tooltip.rcc.generator.hint.bce")
                .withStyle(ChatFormatting.DARK_GRAY));
        // Only a BCE rung joins the D23 array; the item drip has nothing to stack onto a bus.
        if (!tier.outputIsItems()) {
            tooltip.add(Component.translatable("tooltip.rcc.generator.array")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * The rung's {@code 9^n} per-second mint, drawn as a power tower so the exponent sits above the nine rather than
     * after a caret. {@link GeneratorTier#rateTower()} is {@code null} for the item-output rungs, whose rate is a
     * countable number and stays in {@link #appendHoverText}.
     */
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        BceNotation.Tower tower = tierOf(stack).rateTower();
        if (tower == null) {
            return Optional.empty();
        }
        return Optional.of(new TowerTooltipComponent(Component.translatable("tooltip.rcc.generator.rate.label"),
                tower, Component.translatable("tooltip.rcc.generator.rate.unit")));
    }

    /** The rung this stack places as: read off the {@code block_state} component, defaulting to {@link GeneratorTier#BASE}. */
    private GeneratorTier tierOf(ItemStack stack) {
        BlockItemStateProperties stateProps = stack.get(DataComponents.BLOCK_STATE);
        if (stateProps != null) {
            GeneratorTier fromState = stateProps.get(CobblestoneGeneratorBlock.TIER);
            if (fromState != null) {
                return fromState;
            }
        }
        return GeneratorTier.BASE;
    }
}
