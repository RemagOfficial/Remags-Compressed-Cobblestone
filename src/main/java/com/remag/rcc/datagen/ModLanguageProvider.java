package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.GeneratorTier;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, RCC.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // One base name for the single block; per-level names come from the item's getName() override.
        addBlock(ModBlocks.COMPRESSED_COBBLESTONE, "Compressed Cobblestone");
        // Compression Manipulator structure blocks.
        addBlock(ModBlocks.FRAME, "Compression Frame");
        addBlock(ModBlocks.GLASS, "Compression Glass");
        addBlock(ModBlocks.PORT, "Compression Port");
        // Port kind names: the wrench cycles a placed port through these, and the item label and chat feedback
        // both quote them (PortType.displayName), mirroring the pipe mode names above.
        add("block.rcc.compression_port.power", "Power");
        add("block.rcc.compression_port.item_in", "Item Input");
        add("block.rcc.compression_port.item_out", "Item Output");
        addBlock(ModBlocks.INTERFACE, "Compression Interface");
        addBlock(ModBlocks.CORE, "Compression Core");
        addBlock(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE, "Decompression Core");
        addBlock(ModBlocks.COMPRESSION_AUTO, "Compression Auto-Compressor");
        addBlock(ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR, "Auto Decompressor");
        // BCE logistics. The pipe's item name is composed at runtime from these keys - "BCE Pipe" + a literal
        // " III" - because tier is a block state, not a separate registration. Direction is per end and set in world,
        // so it never reaches the item label.
        addBlock(ModBlocks.BCE_PIPE, "BCE Pipe");
        add("block.rcc.bce_pipe.extract", "Extract");
        add("block.rcc.bce_pipe.insert", "Insert");
        add("block.rcc.bce_pipe.passive", "Passive");
        addBlock(ModBlocks.BCE_STORAGE, "BCE Storage");
        addBlock(ModBlocks.BCE_STORAGE_2, "BCE Storage II");
        addItem(ModItems.WRENCH, "Compression Wrench");
        // Cobblestone generators: one registered block spans the whole output ladder, so there is a single base name
        // (the custom item composes the per-rung suffix from GeneratorTier.displaySuffix()) and a shared tooltip built
        // from the rung's rate plus a mode hint. No per-tier lang keys are needed - the item derives them at runtime.
        addBlock(ModBlocks.COBBLESTONE_GENERATOR, "Cobblestone Generator");
        add("tooltip.rcc.generator.rate", "Output: %s");
        add("tooltip.rcc.generator.hint.items", "Emits cobblestone items - extract with a hopper or item pipe.");
        add("tooltip.rcc.generator.hint.bce", "Emits raw BCE - drain it with an extract-mode BCE pipe.");
        add("chat.rcc.generator.cycled", "Generator %1$s - %2$s");
        // The tier-1 generator multiblock: one core block whose rung suffixes are composed at runtime by
        // GeneratorCoreItem (like the single generator above), a shared rate tooltip line, its own FE price line,
        // and the sneak-click status readout. The structure hint spells out the 3x3x4 contract so the tower is
        // buildable without docs.
        addBlock(ModBlocks.GENERATOR_CORE, "Generator Core");
        add("tooltip.rcc.generator_core.cost", "Power draw: %s FE/t while minting");
        add("tooltip.rcc.generator_core.hint",
                "Sealed inside a 3x3x4 frame-and-glass tower with a 1x1 air shaft above or below the core: 1+ Power port, exactly 1 Item-Output (BCE-out) port, and exactly 1 Interface panel (right-click it for status)");
        add("chat.rcc.generator_core.status", "Generator Core %1$s - %2$s");
        // BCE value readout shared by the compressed-cobblestone item tooltip and the vanilla-cobblestone tooltip hook
        // (vanilla shows "BCE: 1"). The value string is composed in DisplayNames.bceValueText.
        add("tooltip.rcc.bce", "BCE: %s");
        // Cobblestone-generator progression line: one advancement per rung. The titles/descriptions come from the very
        // helpers ModAdvancementProvider uses to build the chain, so the lang text and the generated JSON can never
        // drift apart. BASE is the hand-named root ("Only the Beginning"); every other rung is named for the milestone
        // block it asks for.
        // The tab root's title is what the advancements screen shows as the TAB NAME.
        add("advancement.rcc.generator.root", "Remag's Compressed Cobblestone");
        add("advancement.rcc.generator.root.description",
                "It begins, as everything must, with a single piece of cobblestone.");
        for (GeneratorTier tier : GeneratorTier.values()) {
            add("advancement.rcc." + tier.getSerializedName(), ModAdvancementProvider.titleFor(tier));
            add("advancement.rcc." + tier.getSerializedName() + ".description",
                    ModAdvancementProvider.descriptionFor(tier));
        }
        // Sneak-right-click readouts and the wrench click feedback; neither logistics block has a GUI. The mode
        // message names the tier, the end clicked, and its new direction, because a wrench now re-points one face
        // rather than the whole segment. The no-link message answers a click on an end that touches nothing.
        add("chat.rcc.bce_storage", "BCE storage: %1$s / %2$s");
        add("chat.rcc.bce_network.status",
                "BCE pipes: %1$s source(s), %2$s sink(s), moved %3$s last transfer, %4$s leg(s) refused");
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
        // The second tab carries everything mechanical; the mod-name prefix keeps the pair grouped together in the
        // creative screen's tooltip/search.
        add("itemGroup.rcc_machines_tab", "RCC - Machines & Logistics");
        // Compression Manipulator GUI (opened through the interface block).
        add("container.rcc.compression_core", "Compression Manipulator");
        add("container.rcc.compression_core.unformed", "Structure not complete");
        add("container.rcc.compression_core.detached", "No Compression Core found");
        // Decompression Manipulator (the 3x3x3 reverse multiblock), opened through the same interface block.
        add("container.rcc.decompression_core", "Decompression Manipulator");
        // Auto Decompressor goal-tier field.
        add("container.rcc.compression_core.goal", "Goal tier:");
        // JEI Compression Manipulator category.
        add("jei.rcc.manipulator.title", "Compression Manipulator");
        add("jei.rcc.manipulator.info.doubling", "Doubles the seed tier: n \u2192 2n");
        add("jei.rcc.manipulator.info.energy", "Uses %s FE per cycle");
        add("jei.rcc.manipulator.info.fuel_value", "Fuel needed: ~%s cobblestone mass");
        // JEI item information pages (opened with the 'i' key) documenting the processes.
        add("jei.rcc.info.compressed.line1", "Nine of the same tier crafted in a 3x3 grid compress into one block of the next tier up.");
        add("jei.rcc.info.compressed.line2", "Crafting a single block on its own decompresses it back into nine of the tier below.");
        add("jei.rcc.info.compressed.line3", "The tier is stored on the item, so this works for every level - the recipe list only shows a few examples.");
        add("jei.rcc.info.compressed.line4", "The manual 3x3 chain tops out at tier 1000; go beyond it with the Compression Manipulator.");
        add("jei.rcc.info.core.line1", "The centre of the 5x5x5 Compression Manipulator multiblock.");
        add("jei.rcc.info.core.line2", "Doubles a seed's tier: a tier n block becomes tier 2n.");
        add("jei.rcc.info.core.line3", "Needs a complete structure, FE supplied through a Power port, and enough compressed cobblestone fed as fuel.");
        add("jei.rcc.info.core.line4", "Accepts seeds from tier 1000 and up; the output tier is capped around 1 trillion.");
        add("jei.rcc.info.interface.line1", "The GUI entry point. Right-click this panel to open the Compression Manipulator.");
        add("jei.rcc.info.interface.line2", "The sealed core has no interface of its own, and exactly one Interface is required.");
        add("jei.rcc.info.port.line1", "Ports link the outside world to the multiblock; each kind has one job:");
        add("jei.rcc.info.port.line2", "Power ports accept FE, and Item-Input ports take in compressed cobblestone as fuel.");
        add("jei.rcc.info.port.line3", "Item-Output ports eject the finished doubled block.");
        add("jei.rcc.info.port.line4", "Right-click a placed port with a wrench to cycle it between the three kinds.");
    }
}
