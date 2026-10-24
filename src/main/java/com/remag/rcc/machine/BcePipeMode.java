package com.remag.rcc.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * What one <em>end</em> of a {@link BcePipeBlock} does - the BCE equivalent of the import/export sides other mods'
 * pipes have. This is per connection, not per pipe: a single segment can pull from a tank on its north end and push
 * into a machine on its west one, and each of the six ends is configured independently.
 *
 * <ul>
 *   <li>{@link #OFF} - nothing attachable on that side, so the end draws no arm and moves no BCE.</li>
 *   <li>{@link #EXTRACT} - pulls a proportional slice out of every BCE store it touches, into the network.</li>
 *   <li>{@link #INSERT} - receives the network's flow and pushes it into every BCE store it touches.</li>
 *   <li>{@link #PASSIVE} - attached, so it draws an arm (sized like the hub) and joins the run, but is deliberately
 *       inert: it neither pulls nor pushes. For a store you want reachable but quiet, or to cap an end on a line.</li>
 * </ul>
 *
 * <p>{@link #OFF} is part of this enum rather than a separate "connected" flag because the two are the same fact: an
 * end is only extract, insert or passive <em>while it is attached to something</em>, so encoding them together makes
 * "configured but pointing at air" unrepresentable instead of merely discouraged. It also keeps a pipe's state
 * definition at seven properties rather than twelve (2 tiers x 4^6 ends, not 2 x 2^6 x 2^6).
 *
 * <p>Pipes themselves hold nothing: a network is a bus, so <em>all</em> extract ends feed <em>all</em> insert ends
 * regardless of where they sit on the run (see {@link com.remag.rcc.energy.BceNetwork}). A pipe with neither an
 * extract nor an insert end on it simply moves nothing.
 *
 * <p>The mode is chosen in world, not in the crafting grid: one pipe item serves both directions and a wrench
 * {@link BcePipeBlock#useItemOn toggles} the end it is used on, which is why this enum is a state property rather
 * than a second registration.
 */
public enum BcePipeMode implements StringRepresentable {
    /** This end is not attached to anything; no arm is drawn and no BCE moves through it. */
    OFF("off"),
    EXTRACT("extract"),
    INSERT("insert"),
    /** Attached and shows a hub-sized arm, but moves no BCE - the deliberately-inert third option. */
    PASSIVE("passive");

    private final String name;

    BcePipeMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * The next setting a wrench click moves this end to, cycling extract -> insert -> passive -> extract. Only ever
     * called on a linked end - an {@link #OFF} one has nothing to re-point (see {@link BcePipeBlock#useItemOn}) - so
     * {@code OFF} is listed only to keep the switch total and reads back as extract.
     */
    public BcePipeMode toggle() {
        return switch (this) {
            case EXTRACT -> INSERT;
            case INSERT -> PASSIVE;
            case PASSIVE -> EXTRACT;
            case OFF -> EXTRACT;
        };
    }

    /**
     * Whether this end is attached to something, i.e. draws an arm and counts toward the run's hitbox. {@link #PASSIVE}
     * is linked (it has an arm) even though it moves no BCE; only {@link #OFF} lacks one.
     */
    public boolean isLinked() {
        return this != OFF;
    }

    /**
     * The player-facing name of this mode ("Extract" / "Insert" / "Passive"), shared by the click feedback. Never
     * called for {@link #OFF}, which is a lack of configuration rather than a choice, so it has no lang entry.
     */
    public Component displayName() {
        return Component.translatable("block.rcc.bce_pipe." + getSerializedName());
    }
}
