package com.remag.rcc.machine;

import com.remag.rcc.registry.CompressionConfig;
import net.minecraft.util.StringRepresentable;

/**
 * Throughput grade of a {@link BcePipeBlock}. Two tiers, selected by the {@code tier} block state, one per machine band.
 *
 * <p>Each tier moves up to an <strong>absolute per-transfer ceiling</strong> of BCE, taken from
 * {@link CompressionConfig#BCE_PIPE_MAX_TRANSFER_LOG} by ordinal. A pipe never moves more than that ceiling and never
 * more than its source holds, whichever is smaller.
 *
 * <ul>
 *   <li>{@link #TIER_1} &mdash; the Auto-Compressor band: ceiling = one tier-{@code 1000} block's BCE (up to
 *       {@code 1000x} compressed cobblestone);</li>
 *   <li>{@link #TIER_2} &mdash; the Manipulator band: ceiling = one tier-{@code 1e12} block's BCE (up to about
 *       {@code 1 trillion x} compressed cobblestone).</li>
 * </ul>
 *
 * <p>An absolute ceiling (rather than a fraction of the source) is what lets a huge source feed a much smaller sink:
 * the pair no longer has to be within a fixed number of orders to meet. A transfer below the buffer's log10 precision
 * still runs and may duplicate an invisible sliver of mass - accepted, because the end goal is so many orders away
 * that it is nothing.
 */
public enum BcePipeTier implements StringRepresentable {
    TIER_1("one"),
    TIER_2("two");

    private final String name;

    BcePipeTier(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** This tier's absolute per-transfer BCE ceiling, in log10 BCE (see {@link CompressionConfig#BCE_PIPE_MAX_TRANSFER_LOG}). */
    public double maxTransferLog() {
        return CompressionConfig.BCE_PIPE_MAX_TRANSFER_LOG[this.ordinal()];
    }

    /** 1-based tier number, for names and readouts. */
    public int number() {
        return this.ordinal() + 1;
    }
}
