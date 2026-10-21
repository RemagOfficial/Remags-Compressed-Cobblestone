package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
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
        addBlock(ModBlocks.INTERFACE, "Compression Interface");
        addBlock(ModBlocks.CORE, "Compression Core");
        add("itemGroup.rcctab", "Remag's Compressed Cobblestone");
        // Compression Manipulator GUI (opened through the interface block).
        add("container.rcc.compression_core", "Compression Manipulator");
        add("container.rcc.compression_core.unformed", "Structure not complete");
        add("container.rcc.compression_core.detached", "No Compression Core found");
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
    }
}
