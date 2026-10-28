package com.remag.rcc.item;

import com.remag.rcc.machine.CompressionPortBlock;
import com.remag.rcc.machine.PortType;
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

public class CompressionPortItem extends BlockItem {

    public CompressionPortItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static PortType carriedType(ItemStack stack) {
        BlockItemStateProperties stateProps = stack.get(DataComponents.BLOCK_STATE);
        if (stateProps != null) {
            PortType fromState = stateProps.get(CompressionPortBlock.PORT_TYPE);
            if (fromState != null) {
                return fromState;
            }
        }
        return PortType.POWER;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId())
                .append(Component.literal(" - "))
                .append(carriedType(stack).displayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                               TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.rcc.compression_port").withStyle(ChatFormatting.GRAY));
    }
}
