package com.remag.rcc.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * The block tags RCC uses for world logic rather than for crafting recipes.
 *
 * <p>{@link #BCE_CONNECTABLE} exists because a pipe's connections are <em>block state</em>: they are computed once on
 * the server and again on the client during a state update, so the rule that decides them has to be answerable from
 * static data alone. Asking "does that block expose a {@code BCE} capability right now" would be the more precise
 * question, but it cannot be answered during a state update - on the client the neighbouring block entity may not
 * have arrived yet, and any answer that differs between sides produces states that flicker or arms that never appear.
 * A tag is the same data on both sides by construction, and it doubles as the compat hook: a pack or another mod
 * adds its own machine to the tag and pipes immediately bolt an end to it.
 */
public final class ModBlockTags {
    /**
     * Blocks a BCE pipe may bolt an end to because BCE can actually move into or out of them - every block that
     * registers {@link ModCapabilities#BCE} for its block entity, except pipes themselves (which are covered by the
     * pipe-to-pipe rule). Keep this in sync with the registrations in {@link com.remag.rcc.event.CommonEvents}; a
     * block in the tag with no capability draws an arm that moves nothing.
     */
    public static final TagKey<Block> BCE_CONNECTABLE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("rcc", "bce_connectable"));

    private ModBlockTags() {
    }
}
