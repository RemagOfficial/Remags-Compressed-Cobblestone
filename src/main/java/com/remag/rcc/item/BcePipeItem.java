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
