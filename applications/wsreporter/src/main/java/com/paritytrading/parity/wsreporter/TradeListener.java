package com.paritytrading.parity.wsreporter;

import java.util.Locale;

abstract class TradeListener {

    public abstract void trade(Trade event);

    protected void printf(String format, Object... args) {
        System.out.printf(Locale.US, format, args);
    }

}
