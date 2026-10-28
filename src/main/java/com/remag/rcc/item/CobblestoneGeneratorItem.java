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

public class CobblestoneGeneratorItem extends BlockItem {

    public CobblestoneGeneratorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.rcc.cobblestone_generator")
                .append(Component.literal(" (" + tierOf(stack).displaySuffix() + ")"));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        GeneratorTier tier = tierOf(stack);
        if (tier.rateTower() == null) {
            tooltip.add(Component.translatable("tooltip.rcc.generator.rate", tier.describe())
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(
                        tier.outputIsItems() ? "tooltip.rcc.generator.hint.items" : "tooltip.rcc.generator.hint.bce")
                .withStyle(ChatFormatting.DARK_GRAY));
        if (!tier.outputIsItems()) {
            tooltip.add(Component.translatable("tooltip.rcc.generator.array")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        BceNotation.Tower tower = tierOf(stack).rateTower();
        if (tower == null) {
            return Optional.empty();
        }
        return Optional.of(new TowerTooltipComponent(Component.translatable("tooltip.rcc.generator.rate.label"),
                tower, Component.translatable("tooltip.rcc.generator.rate.unit")));
    }

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
