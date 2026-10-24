package com.remag.rcc.item;

import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeTier;
import com.remag.rcc.registry.DisplayNames;
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
 * The single item behind every BCE pipe. The tier rides on the stack's vanilla {@code block_state} component (exactly
 * how {@link com.remag.rcc.machine.CompressionPortBlock} carries its three kinds), so one registration covers the
 * whole ladder: {@code "BCE Pipe II"}.
 *
 * <p>The <em>direction</em> is not part of the item at all: it lives on each of the six ends and is configured in
 * world with a wrench (see {@link BcePipeBlock}), so a stack's label carries only its tier - there is no single
 * "this pipe's mode" to name any more than there is a way to say which end the player intends to point.
 */
public class BcePipeItem extends BlockItem {

    public BcePipeItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        BcePipeTier tier = BcePipeTier.TIER_1;
        BlockItemStateProperties stateProps = stack.get(DataComponents.BLOCK_STATE);
        if (stateProps != null) {
            BcePipeTier fromState = stateProps.get(BcePipeBlock.TIER);
            if (fromState != null) {
                tier = fromState;
            }
        }
        // "BCE Pipe II". The separator is a literal rather than part of the lang value because a .properties value
        // loses any leading whitespace when loaded.
        return Component.translatable("block.rcc.bce_pipe")
                .append(Component.literal(" " + DisplayNames.roman(tier.number())));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.rcc.bce_pipe").withStyle(ChatFormatting.GRAY));
    }
}
