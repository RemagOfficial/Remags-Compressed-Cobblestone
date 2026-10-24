package com.remag.rcc.item;

import com.remag.rcc.machine.GeneratorCoreBlock;
import com.remag.rcc.machine.GeneratorCoreTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * The single item behind every tier-1 generator rung. The rung rides on the stack's vanilla {@code block_state}
 * component (the {@link CobblestoneGeneratorItem} route), so one registration covers the whole ladder while each
 * creative entry still reads distinctly: {@code "Generator Core (I)"} .. {@code "Generator Core (X)"}.
 *
 * <p>The hover text names the rung's per-second mint and its FE/t price - the two numbers that define where a
 * Generation Manipulator sits on the ladder - and points at the 3x3x4 tower it must be sealed inside.
 */
public class GeneratorCoreItem extends BlockItem {

    public GeneratorCoreItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        // "Generator Core (VII)" - the roman rung suffix is composed, not a raw lang value (the pipe/port pattern).
        return Component.translatable(getDescriptionId())
                .append(Component.literal(" (" + rungOf(stack).displaySuffix() + ")"));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        GeneratorCoreTier rung = rungOf(stack);
        tooltip.add(Component.translatable("tooltip.rcc.generator.rate", rung.describe())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rcc.generator_core.cost",
                        String.format(java.util.Locale.US, "%,d", rung.energyPerTick()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rcc.generator_core.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** The rung this stack places as: read off the {@code block_state} component, defaulting to the ladder's base. */
    private GeneratorCoreTier rungOf(ItemStack stack) {
        BlockItemStateProperties stateProps = stack.get(DataComponents.BLOCK_STATE);
        if (stateProps != null) {
            GeneratorCoreTier fromState = stateProps.get(GeneratorCoreBlock.RUNG);
            if (fromState != null) {
                return fromState;
            }
        }
        return GeneratorCoreTier.T1_P1000;
    }
}
