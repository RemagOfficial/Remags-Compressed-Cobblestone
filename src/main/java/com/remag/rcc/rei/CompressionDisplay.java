package com.remag.rcc.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;

import java.util.ArrayList;
import java.util.List;

public class CompressionDisplay implements Display {

    private final List<EntryIngredient> recipeInputs;
    private final List<EntryIngredient> outputs;
    private final List<EntryIngredient> allInputs;

    public CompressionDisplay(List<EntryIngredient> recipeInputs, List<EntryIngredient> outputs,
                              List<EntryIngredient> workstations) {
        this.recipeInputs = recipeInputs;
        this.outputs = outputs;
        List<EntryIngredient> combined = new ArrayList<>(recipeInputs);
        combined.addAll(workstations);
        this.allInputs = List.copyOf(combined);
    }

    public List<EntryIngredient> getRecipeInputs() {
        return recipeInputs;
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
        return CompressionCategory.IDENTIFIER;
    }
}
