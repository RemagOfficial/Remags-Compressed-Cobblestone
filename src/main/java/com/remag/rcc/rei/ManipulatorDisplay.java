package com.remag.rcc.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;

import java.util.ArrayList;
import java.util.List;

public class ManipulatorDisplay implements Display {

    private final List<EntryIngredient> inputs;
    private final List<EntryIngredient> outputs;
    private final List<EntryIngredient> allInputs;
    private final int energyPerCraft;
    private final double requiredFuelLog;

    public ManipulatorDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs,
                              List<EntryIngredient> workstations, int energyPerCraft, double requiredFuelLog) {
        this.inputs = inputs;
        this.outputs = outputs;
        List<EntryIngredient> combined = new ArrayList<>(inputs);
        combined.addAll(workstations);
        this.allInputs = List.copyOf(combined);
        this.energyPerCraft = energyPerCraft;
        this.requiredFuelLog = requiredFuelLog;
    }

    public List<EntryIngredient> getRecipeInputs() {
        return inputs;
    }

    public int energyPerCraft() {
        return energyPerCraft;
    }

    public double requiredFuelLog() {
        return requiredFuelLog;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return allInputs;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return outputs;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return ManipulatorCategory.IDENTIFIER;
    }
}
