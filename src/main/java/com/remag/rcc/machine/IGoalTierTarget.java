package com.remag.rcc.machine;

import java.math.BigInteger;

public interface IGoalTierTarget {
    BigInteger getGoalTier();

    void setGoalTier(BigInteger goalTier);

    String goalTierString();
}
