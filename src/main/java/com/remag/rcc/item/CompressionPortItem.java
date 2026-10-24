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

/**
 * The single item behind every {@link CompressionPortBlock} kind. The intended type rides on the stack's vanilla
 * {@code block_state} component (the same trick as {@link BcePipeItem}'s tier and
 * {@link com.remag.rcc.item.CobblestoneGeneratorItem}'s rung), which is what the three creative-tab entries
 * pre-set and what {@link CompressionPortBlock#getStateForPlacement} reads back on placement.
 *
 * <p>The label carries the kind - {@code "Compression Port - Power"} - because the three tab entries would otherwise
 * be indistinguishable. A placed port, however, is re-pointed in world with a wrench (see
 * {@link CompressionPortBlock#useItemOn}), so the state on the block, not the item, is authoritative once it is in
 * the structure; the tooltip says so.
 */
public class CompressionPortItem extends BlockItem {

    public CompressionPortItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** The kind this stack places as: the {@code block_state} component's type, or {@link PortType#POWER} by default. */
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
        // "Compression Port - Power". The separator is a literal rather than part of the lang value because a
        // .properties value loses any leading whitespace when loaded.
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
