package com.remag.rcc.item;

import com.remag.rcc.machine.GeneratorCoreBlock;
import com.remag.rcc.machine.GeneratorCoreTier;
import com.remag.rcc.registry.CompressionBand;
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

public class GeneratorCoreItem extends BlockItem {

    public GeneratorCoreItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId())
                .append(Component.literal(" (" + rungOf(stack).displaySuffix() + ")"));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        GeneratorCoreTier rung = rungOf(stack);
        tooltip.add(Component.translatable("tooltip.rcc.generator_core.cost",
                        String.format(java.util.Locale.US, "%,d", rung.energyPerTick()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rcc.generator_core.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.rcc.generator.array")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new TowerTooltipComponent(Component.translatable("tooltip.rcc.generator.rate.label"),
                rungOf(stack).rateTower(),
                Component.translatable("tooltip.rcc.generator.rate.unit")));
    }

    private GeneratorCoreTier rungOf(ItemStack stack) {
        CompressionBand band = getBlock() instanceof GeneratorCoreBlock core
                ? core.band() : CompressionBand.BAND_1;
        BlockItemStateProperties stateProps = stack.get(DataComponents.BLOCK_STATE);
        Integer index = stateProps == null ? null : stateProps.get(GeneratorCoreBlock.RUNG);
        return new GeneratorCoreTier(band, index == null ? 0 : index);
    }
}
