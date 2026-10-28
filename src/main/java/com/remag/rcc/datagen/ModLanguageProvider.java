package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.GeneratorTier;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.DisplayNames;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, RCC.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        addBlock(ModBlocks.COMPRESSED_COBBLESTONE, "Compressed Cobblestone");
        addBlock(ModBlocks.FRAME, "Compression Frame");
        addBlock(ModBlocks.GLASS, "Compression Glass");
        addBlock(ModBlocks.PORT, "Compression Port");
        add("block.rcc.compression_port.power", "Power");
        add("block.rcc.compression_port.item_in", "Item Input");
        add("block.rcc.compression_port.item_out", "Item Output");
        add("block.rcc.compression_port.seed", "Seed Input");
        addBlock(ModBlocks.INTERFACE, "Compression Interface");
        addBlock(ModBlocks.CORE, "Compression Core");
        addBlock(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE, "Decompression Core");
        addBlock(ModBlocks.COMPRESSION_CORE_2, "Compression Core II");
        addBlock(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_2, "Decompression Core II");
        for (CompressionBand band : CompressionBand.values()) {
            if (band.index() < 3) {
                continue;
            }
            String numeral = DisplayNames.roman(band.index());
            add(blockKey(ModBlocks.COMPRESSION_CORES_BY_BAND.get(band)), "Compression Core " + numeral);
            add(blockKey(ModBlocks.GENERATOR_CORES_BY_BAND.get(band)), "Generator Core " + numeral);
            add(blockKey(ModBlocks.BCE_STORAGES_BY_BAND.get(band)),
                    "BCE Storage " + DisplayNames.roman(band.index() + 1));
        }
        addBlock(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_3, "Decompression Core III");
        addBlock(ModBlocks.COMPRESSION_AUTO, "Compression Auto-Compressor");
        addBlock(ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR, "Auto Decompressor");
        addBlock(ModBlocks.COMPRESSION_EDITOR, "Compression Editor");
        addBlock(ModBlocks.BCE_PIPE, "BCE Pipe");
        add("block.rcc.bce_pipe.extract", "Extract");
        add("block.rcc.bce_pipe.insert", "Insert");
        add("block.rcc.bce_pipe.passive", "Passive");
        addBlock(ModBlocks.BCE_STORAGE, "BCE Storage");
        addBlock(ModBlocks.BCE_STORAGE_2, "BCE Storage II");
        addBlock(ModBlocks.BCE_STORAGE_3, "BCE Storage III");
        addBlock(ModBlocks.CREATIVE_BCE_SOURCE, "Creative BCE Source");
        addItem(ModItems.WRENCH, "Compression Wrench");
        addBlock(ModBlocks.COBBLESTONE_GENERATOR, "Cobblestone Generator");
        add("tooltip.rcc.generator.rate", "Output: %s");
        add("tooltip.rcc.generator.rate.label", "Output:");
        add("tooltip.rcc.generator.rate.unit", "BCE/sec");
        add("tooltip.rcc.generator.hint.items", "Emits cobblestone items - extract with a hopper or item pipe.");
        add("tooltip.rcc.generator.hint.bce", "Emits raw BCE - drain it with an extract-mode BCE pipe.");
        add("tooltip.rcc.generator.array",
                "Stack 2^(band+1) towers on one BCE bus to mint a whole band-max block per second");
        add("chat.rcc.generator.cycled", "Generator %1$s - %2$s");
        addBlock(ModBlocks.GENERATOR_CORE, "Generator Core");
        addBlock(ModBlocks.GENERATOR_CORE_2, "Generator Core II");
        add("tooltip.rcc.generator_core.cost", "Power draw: %s FE/t while minting");
        add("tooltip.rcc.generator_core.hint",
                "Sealed inside a 3x3x4 frame-and-glass tower with a 1x1 air shaft above or below the core: 1+ Power port, exactly 1 Item-Output (BCE-out) port, and exactly 1 Interface panel (right-click it for status)");
        add("chat.rcc.generator_core.status", "Generator Core %1$s - %2$s");
        add("tooltip.rcc.bce", "BCE: %s");
        add("tooltip.rcc.bce.label", "BCE:");
        add("advancement.rcc.generator.root", "Remag's Compressed Cobblestone");
        add("advancement.rcc.generator.root.description",
                "It begins, as everything must, with a single piece of cobblestone.");
        for (GeneratorTier tier : GeneratorTier.values()) {
            add("advancement.rcc." + tier.getSerializedName(), ModAdvancementProvider.titleFor(tier));
            add("advancement.rcc." + tier.getSerializedName() + ".description",
                    ModAdvancementProvider.descriptionFor(tier));
        }
        add("advancement.rcc.reward.compression_editor", "The Last Shade");
        add("advancement.rcc.reward.compression_editor.description",
                "Craft the Compression Editor - any shade of cobblestone, forever.");
        add("chat.rcc.bce_storage", "BCE storage: %1$s / %2$s");
        add("chat.rcc.bce_network.status",
                "BCE pipes: %1$s source(s), %2$s sink(s), moved %3$s last transfer, %4$s leg(s) refused, "
                        + "%5$s generator(s) arrayed at %6$s/sec");
        add("chat.rcc.bce_pipe.mode", "BCE Pipe %1$s: %2$s end set to %3$s");
        add("chat.rcc.bce_pipe.no_link", "BCE Pipe has nothing attached on its %s end - connect something first");
        add("chat.rcc.port.cycled", "Compression Port set to %s");
        add("tooltip.rcc.compression_port",
                "Right-click a placed port with a wrench to cycle Power \u2192 Item Input \u2192 Item Output");
        add("side.rcc.down", "Down");
        add("side.rcc.up", "Up");
        add("side.rcc.north", "North");
        add("side.rcc.south", "South");
        add("side.rcc.west", "West");
        add("side.rcc.east", "East");
        add("tooltip.rcc.bce_pipe", "Right-click an end with a wrench to cycle that end Extract \u2192 Insert \u2192 Passive");
        add("itemGroup.rcctab", "Remag's Compressed Cobblestone");
        add("itemGroup.rcc_machines_tab", "RCC - Machines & Logistics");
        add("container.rcc.compression_core", "Compression Manipulator");
        add("container.rcc.compression_core.unformed", "Structure not complete");
        add("container.rcc.compression_core.status.output_full", "Output slot full");
        add("container.rcc.compression_core.status.need_fuel", "Not enough BCE fuel");
        add("container.rcc.compression_core.status.need_energy", "Not enough FE power");
        add("container.rcc.compression_core.detached", "No Compression Core found");
        add("container.rcc.decompression_core", "Decompression Manipulator");
        add("container.rcc.compression_core.goal", "Goal tier:");
        add("container.rcc.compression_editor.tier", "Tier: %s");
        add("container.rcc.compression_editor.slider", "Compression tier slider");
        add("jei.rcc.manipulator.title", "Compression Manipulator");
        add("jei.rcc.manipulator.info.doubling", "Doubles seed tier: n \u2192 2n");
        add("jei.rcc.manipulator.info.energy", "%s FE per cycle");
        add("jei.rcc.manipulator.info.fuel_value", "BCE: ~%s");
        add("jei.rcc.info.compressed.line1", "Craft nine of the same tier in a 3x3 grid to compress them into one block of the next tier up.");
        add("jei.rcc.info.compressed.line2", "Craft a single block on its own to decompress it back into nine of the tier below.");
        add("jei.rcc.info.compressed.line3", "The tier lives on the item, so this works at every level - the recipe list just shows a few examples.");
        add("jei.rcc.info.compressed.line4", "The manual 3x3 chain stops at tier 1000; past that, use the Compression Manipulator.");
        add("jei.rcc.info.core.line1", "The heart of the 5x5x5 Compression Manipulator multiblock.");
        add("jei.rcc.info.core.line2", "Doubles a seed's tier: a tier n block becomes a tier 2n block.");
        add("jei.rcc.info.core.line3", "Needs a complete shell, FE fed through a Power port, and compressed cobblestone burned as fuel.");
        add("jei.rcc.info.core.line4", "Takes seeds from tier 1000 upward; the output tier tops out near 1 trillion.");
        add("jei.rcc.info.interface.line1", "The machine's GUI. Right-click this panel to open the Compression Manipulator.");
        add("jei.rcc.info.interface.line2", "The sealed core can't be clicked, so exactly one Interface is required per structure.");
        add("jei.rcc.info.port.line1", "Ports are the multiblock's only link to the outside world, and each kind has one job:");
        add("jei.rcc.info.port.line2", "Power ports take in FE; Item-Input ports take in compressed cobblestone to burn as fuel.");
        add("jei.rcc.info.port.line3", "Item-Output ports eject the finished block, and Seed ports feed in the seed to double.");
        add("jei.rcc.info.port.line4", "Right-click a placed port with a wrench to cycle it through the four kinds.");
    }

    private static String blockKey(DeferredBlock<? extends net.minecraft.world.level.block.Block> block) {
        return block.get().getDescriptionId();
    }
}
