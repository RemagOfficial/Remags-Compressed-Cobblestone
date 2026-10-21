package com.remag.rcc.item;

import com.remag.rcc.registry.DisplayNames;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.math.BigInteger;

/**
 * The single block item for every compression tier. The tier lives in the {@code compression_level}
 * data component, and the per-level tooltip name is derived from it at render time — replacing the
 * previous one-registered-item-per-level and one-lang-key-per-level schemes.
 */
public class CompressedCobblestoneItem extends BlockItem {

    public CompressedCobblestoneItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        BigInteger level = stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
        return Component.literal(DisplayNames.compressed(level));
    }
}
