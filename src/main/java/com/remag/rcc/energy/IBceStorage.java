package com.remag.rcc.energy;

public interface IBceStorage {
    double getBceStoredLog();

    double getMaxBceLog();

    double receiveBce(double amountLog, boolean simulate);

    double extractBce(double amountLog, boolean simulate);

    boolean canReceive();

    boolean canExtract();
}
