package com.amine.chessmath;

/** Settings for future exercises. Keep a copy with the exercise currently on screen. */
public final class MathConfig {
    public boolean addition = true;
    public boolean subtraction = true;
    public boolean multiplication = true;
    public boolean crossingTen;
    public int limit = 100;
    public boolean[] tables = new boolean[11];

    public MathConfig() {
        tables[1] = tables[2] = tables[5] = tables[10] = true;
    }

    public MathConfig copy() {
        MathConfig copy = new MathConfig();
        copy.addition = addition;
        copy.subtraction = subtraction;
        copy.multiplication = multiplication;
        copy.crossingTen = crossingTen;
        copy.limit = limit;
        copy.tables = tables == null ? null : tables.clone();
        return copy;
    }

    public void validate() {
        if (!addition && !subtraction && !multiplication)
            throw new IllegalArgumentException("Kies minstens één soort som.");
        if (limit != 20 && limit != 50 && limit != 100)
            throw new IllegalArgumentException("Kies 20, 50 of 100 als getalgrens.");
        if (tables == null || tables.length != 11 || tables[0])
            throw new IllegalArgumentException("Kies tafels van 1 tot en met 10.");
        boolean anyTable = false;
        for (int i = 1; i <= 10; i++) anyTable |= tables[i];
        if (multiplication && !anyTable)
            throw new IllegalArgumentException("Kies minstens één tafel of zet vermenigvuldigen uit.");
    }
}
