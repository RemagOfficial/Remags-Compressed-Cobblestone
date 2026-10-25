package com.remag.rcc.machine;

import com.remag.rcc.registry.CompressionBand;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

/**
 * Something that mints BCE and therefore counts toward the <em>generator array</em> of the
 * {@link com.remag.rcc.energy.BceNetwork} it drains into: a single-block
 * {@link CobblestoneGeneratorBlockEntity} or a {@link GeneratorCoreBlockEntity} tower (and, when a later band lands,
 * whatever class its ladder is built from). Both families answer the same two questions so the bus can add them up
 * without knowing which rung enum a store rode in on - the sum is what makes {@code 2^(band + 1)} towers mint a whole
 * band-max block per second instead of one tower doing a half of it (see {@code MASTER_PLAN.md} D23).
 *
 * <p><b>Why a share rather than a rate.</b> The number exchanged here is an <em>exponent</em>, in
 * {@link BigInteger}, not a log10 mass in a {@code double}. That is deliberate: {@code N} generators must produce more
 * than one, and multiplying a mass by {@code N} moves a {@code double} pool by {@code log10(N)} - invisible against a
 * figure whose own resolution is thousands to billions of decades wide (see
 * {@link com.remag.rcc.registry.CompressionConfig#log10add}). Adding exponents is exact, so the array rule lives in
 * exponent space and only the final {@code 9^sum} conversion is a {@code double}.
 *
 * <p>Implementations return {@code null} from {@link #arrayShareExponent()} whenever they are <em>not</em> producing -
 * an incomplete shell, a starved FE buffer, or a rung that still emits physical cobblestone - so a bus's total tracks
 * the machines that are actually running rather than the ones merely placed.
 */
public interface IGeneratorArrayMember {
    /**
     * The exponent this machine contributes to its bus: its rung's milestone exponent divided by {@code 2^band}
     * ({@link CompressionBand#shareOf}), or {@code null} when it is not currently producing and must not be counted.
     */
    @Nullable
    BigInteger arrayShareExponent();

    /**
     * The band this member belongs to. A bus clamps its aggregate at the {@link CompressionBand#maxOutputTier()} of
     * the <em>highest</em> band present, so an array of one band's towers can never out-mint that band's final block.
     */
    CompressionBand arrayBand();
}
