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

    /**
     * The block's BCE value ({@code 9^tier} vanilla cobblestone), drawn as a power tower rather than written out: the
     * tier sits on the {@code compression_level} component, so the whole ladder shares this one item and its tooltip,
     * and a deep milestone's exponent ({@code 10^99}-class past band 1) is a stack of glyphs instead of either a wall of
     * digits or the {@code ^(...)} brackets a plain string has to fall back on. Drawing replaces the old
     * {@code appendHoverText} line, which would otherwise duplicate it - the graphic lands at the end of the tooltip,
     * where vanilla puts every {@code getTooltipImage} payload.
     */
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        BigInteger level = stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE);
        return Optional.of(TowerTooltipComponent.of(Component.translatable("tooltip.rcc.bce.label"),
                DisplayNames.bceValueTower(level)));
    }
}
