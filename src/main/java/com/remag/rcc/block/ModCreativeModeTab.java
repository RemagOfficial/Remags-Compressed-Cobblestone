package com.remag.rcc.block;

import com.remag.rcc.RCC;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeTier;
import com.remag.rcc.machine.CobblestoneGeneratorBlock;
import com.remag.rcc.machine.CompressionPortBlock;
import com.remag.rcc.machine.GeneratorCoreBlock;
import com.remag.rcc.machine.GeneratorCoreTier;
import com.remag.rcc.machine.GeneratorTier;
import com.remag.rcc.machine.PortType;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.math.BigInteger;

public class ModCreativeModeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RCC.MODID);

    /**
     * The namesake tab: the cobblestone ladder itself and nothing else. Everything the player <em>builds or
     * moves mass with</em> lives in {@link #RCC_MACHINES_TAB}, so scrolling a thousand compression tiers never
     * buries the machines (and vice versa).
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RCC_TAB = TABS.register("rcc_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rcctab"))
                    .icon(() -> levelStack(BigInteger.ONE))
                    .displayItems((displayParams, output) -> {
                        // One entry per tier, all sharing the single item but differing by the
                        // compression_level data component. The per-tier name/tint come from that.
                        // The manual 3x3 chain tops out at MANUAL_MAX_LEVEL; bigger tiers come from
                        // the Compression Manipulator multiblock and are not listed here.
                        for (BigInteger level = BigInteger.ONE;
                             level.compareTo(CompressionConfig.MANUAL_MAX_LEVEL) <= 0;
                             level = level.add(BigInteger.ONE)) {
                            output.accept(levelStack(level));
                        }
                    })
                    .build()
    );

    /**
     * The machines-and-logistics tab: everything that makes, shapes, moves or spends mass - both multiblocks'
     * shells and cores, the single-block Auto machines, the BCE pipe network with its tanks, the generators and
     * the wrench. Registered after {@link #RCC_TAB}, so it docks right next to it in the creative screen.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RCC_MACHINES_TAB =
            TABS.register("rcc_machines_tab",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.rcc_machines_tab"))
                            .icon(() -> new ItemStack(ModItems.COMPRESSION_CORE.get()))
                            .displayItems((displayParams, output) -> {
                                // Compression Manipulator structure blocks (placeholder multiblock pieces).
                                output.accept(new ItemStack(ModItems.COMPRESSION_FRAME.get()));
                                output.accept(new ItemStack(ModItems.COMPRESSION_GLASS.get()));
                                output.accept(portStack(PortType.POWER));
                                output.accept(portStack(PortType.ITEM_IN));
                                output.accept(portStack(PortType.ITEM_OUT));
                                output.accept(new ItemStack(ModItems.COMPRESSION_INTERFACE.get()));
                                output.accept(new ItemStack(ModItems.COMPRESSION_CORE.get()));
                                // The tier-1 Decompression Manipulator core (the 3x3x3 reverse multiblock; reuses
                                // the shell blocks above).
                                output.accept(new ItemStack(ModItems.COMPRESSION_DECOMPRESSOR_CORE.get()));
                                // The single-block Auto-Compressor (automates the tier 1..1000 climb).
                                output.accept(new ItemStack(ModItems.COMPRESSION_AUTO.get()));
                                // The single-block Auto Decompressor (the reverse: banks released mass as BCE).
                                output.accept(new ItemStack(ModItems.COMPRESSION_AUTO_DECOMPRESSOR.get()));
                                // BCE logistics: two throughput tiers (each placeable as either end - the direction
                                // is set in world with the wrench), the tanks they fill, and the wrench itself.
                                for (BcePipeTier tier : BcePipeTier.values()) {
                                    output.accept(pipeStack(tier));
                                }
                                output.accept(new ItemStack(ModItems.BCE_STORAGE.get()));
                                output.accept(new ItemStack(ModItems.BCE_STORAGE_2.get()));
                                output.accept(new ItemStack(ModItems.WRENCH.get()));
                                // Cobblestone generators: one entry per rung of the output ladder (item drip up to
                                // the top BCE tier), each preset through the block_state component the block reads
                                // back on place.
                                for (GeneratorTier tier : GeneratorTier.values()) {
                                    output.accept(generatorStack(tier));
                                }
                                // The tier-1 generator multiblock's cores: one entry per rung of the BCE fountain
                                // ladder, each preset the same block_state way (the 3x3x4 tower is built in world).
                                for (GeneratorCoreTier rung : GeneratorCoreTier.values()) {
                                    output.accept(generatorCoreStack(rung));
                                }
                            })
                            .build()
            );

    /** A creative-tab stack for the given tier, carrying the compression_level component. */
    private static ItemStack levelStack(BigInteger level) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), level);
        return stack;
    }

    /** A port stack preset to the given {@link PortType}; {@code getStateForPlacement} reads it back. */
    private static ItemStack portStack(PortType type) {
        ItemStack stack = new ItemStack(ModItems.COMPRESSION_PORT.get());
        stack.set(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(CompressionPortBlock.PORT_TYPE, type));
        return stack;
    }

    /**
     * A pipe stack preset to one tier. All tiers share {@link ModItems#BCE_PIPE}, so the creative tab differentiates
     * them purely through the {@code block_state} component that {@link BcePipeBlock#getStateForPlacement} reads back
     * on placement (the same trick as {@link #portStack}). No mode is set: a fresh pipe's ends are passive until a
     * wrench points them, which is why one entry per tier is enough for every direction.
     */
    private static ItemStack pipeStack(BcePipeTier tier) {
        ItemStack stack = new ItemStack(ModItems.BCE_PIPE.get());
        stack.set(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(BcePipeBlock.TIER, tier));
        return stack;
    }

    /**
     * A generator stack preset to one rung of the ladder. All rungs share {@link ModItems#COBBLESTONE_GENERATOR}, so
     * the creative tab differentiates them through the {@code block_state} component (the same trick as
     * {@link #pipeStack}); the block reads it back on placement to decide output rate and item/BCE mode.
     */
    private static ItemStack generatorStack(GeneratorTier tier) {
        ItemStack stack = new ItemStack(ModItems.COBBLESTONE_GENERATOR.get());
        stack.set(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(CobblestoneGeneratorBlock.TIER, tier));
        return stack;
    }

    /**
     * A tier-1 generator core preset to one rung of the BCE fountain ladder. All rungs share
     * {@link ModItems#GENERATOR_CORE} and are differentiated through the {@code block_state} component like
     * {@link #generatorStack}; the tower block reads it back on placement to decide rate and FE price.
     */
    private static ItemStack generatorCoreStack(GeneratorCoreTier rung) {
        ItemStack stack = new ItemStack(ModItems.GENERATOR_CORE.get());
        stack.set(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(GeneratorCoreBlock.RUNG, rung));
        return stack;
    }
}
