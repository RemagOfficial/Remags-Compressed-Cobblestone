package com.remag.rcc.machine;

import com.remag.rcc.registry.CompressionBand;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public interface IGeneratorArrayMember {
    @Nullable
    BigInteger arrayShareExponent();

    CompressionBand arrayBand();
}
