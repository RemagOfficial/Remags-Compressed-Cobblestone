package com.remag.rcc.item;

import com.remag.rcc.registry.DisplayNames;
import com.remag.rcc.registry.ModDataComponents;
import com.remag.rcc.registry.TowerTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.math.BigInteger;
import java.util.Optional;

public class CompressedCobblestoneItem extends BlockItem {

    public CompressedCobblestoneItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        BigInteger level = stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
        return Component.literal(DisplayNames.compressed(level));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        BigInteger level = stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
        return Optional.of(TowerTooltipComponent.of(Component.translatable("tooltip.rcc.bce.label"),
                DisplayNames.bceValueTower(level)));
    }
}
