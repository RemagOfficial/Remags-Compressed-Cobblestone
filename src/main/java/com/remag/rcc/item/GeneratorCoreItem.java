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

/**
 * The single item behind every tier-1 generator rung. The rung rides on the stack's vanilla {@code block_state}
 * component (the {@link CobblestoneGeneratorItem} route), so one registration covers the whole ladder while each
 * creative entry still reads distinctly: {@code "Generator Core (I)"} .. {@code "Generator Core (XIII)"}.
 *
 * <p>The hover text names the rung's per-second mint and its FE/t price - the two numbers that define where a
 * Generation Manipulator sits on the ladder - and points at the 3x3x4 tower it must be sealed inside. The mint is
 * {@code 9^n} with an {@code n} that runs from {@code 1000} to {@code 5*10^14} and past, so it is <em>drawn</em> as a
 * power tower by {@link #getTooltipImage(ItemStack)} instead of written as a text line: top-of-ladder rungs need two
 * raised levels, which no font string can express.
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
        // The rate line is drawn by getTooltipImage; only the two figures that are plain numbers stay as text.
        tooltip.add(Component.translatable("tooltip.rcc.generator_core.cost",
                        String.format(java.util.Locale.US, "%,d", rung.energyPerTick()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rcc.generator_core.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        // The tower mints only its rung's share; the array rule (2^(band+1) towers = one whole block/sec) is what the
        // single-machine tooltip cannot measure, so it is stated rather than shown.
        tooltip.add(Component.translatable("tooltip.rcc.generator.array")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * The rung's {@code 9^n} mint, drawn: label, then the exponent raised over the base, then the unit back on the
     * baseline. Every core rung is a pure BCE producer, so unlike {@link CobblestoneGeneratorItem} there is no
     * countable-output case to fall back to text - {@link GeneratorCoreTier#describe()} flattens the same tower for
     * the chat readout.
     */
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new TowerTooltipComponent(Component.translatable("tooltip.rcc.generator.rate.label"),
                rungOf(stack).rateTower(),
                Component.translatable("tooltip.rcc.generator.rate.unit")));
    }

    /** The rung this stack places as: this item's band plus the index on its {@code block_state} component. */
    private GeneratorCoreTier rungOf(ItemStack stack) {
        CompressionBand band = getBlock() instanceof GeneratorCoreBlock core
                ? core.band() : CompressionBand.BAND_1;
        BlockItemStateProperties stateProps = stack.get(DataComponents.BLOCK_STATE);
        Integer index = stateProps == null ? null : stateProps.get(GeneratorCoreBlock.RUNG);
        return new GeneratorCoreTier(band, index == null ? 0 : index);
    }
}
