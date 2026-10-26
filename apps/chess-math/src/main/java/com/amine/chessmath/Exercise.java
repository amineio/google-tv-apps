package com.amine.chessmath;

/** Immutable operands make hints and recovered answers refer to the same exercise. */
public final class Exercise {
    public static final String ADD = "ADD", SUBTRACT = "SUBTRACT", MULTIPLY = "MULTIPLY";
    public final long id;
    public final int a, b;
    public final String operation;

    public Exercise(long id, int a, int b, String operation) {
        if (id < 0 || a < 0 || b < 0 || a > 100 || b > 100)
            throw new IllegalArgumentException("Ongeldige som.");
        if (!(ADD.equals(operation) || SUBTRACT.equals(operation) || MULTIPLY.equals(operation)))
            throw new IllegalArgumentException("Onbekende bewerking.");
        int result = ADD.equals(operation) ? a + b : SUBTRACT.equals(operation) ? a - b : a * b;
        if (result < 0 || result > 100) throw new IllegalArgumentException("Antwoord buiten 0 tot en met 100.");
        this.id = id; this.a = a; this.b = b; this.operation = operation;
    }

    public int answer() {
        return ADD.equals(operation) ? a + b : SUBTRACT.equals(operation) ? a - b : a * b;
    }

    public String expression() {
        return a + (ADD.equals(operation) ? " + " : SUBTRACT.equals(operation) ? " − " : " × ") + b;
    }

    public String hint(int level) {
        if (level >= 2) return expression() + " = " + answer();
        if (MULTIPLY.equals(operation))
            return "Teken " + a + " groepjes van " + b + ". Tel de groepjes bij elkaar op.";
        if (ADD.equals(operation)) {
            int toTen = 10 - a % 10;
            if (a % 10 + b % 10 >= 10)
                return "Splits " + b + " in " + toTen + " en " + (b - toTen)
                        + ". Tel eerst " + toTen + " bij " + a + ". Tel daarna de rest erbij.";
            return "Splits " + b + " in " + (b / 10 * 10) + " en " + (b % 10)
                    + ". Tel eerst de tientallen bij " + a + ". Tel daarna de eenheden erbij.";
        }
        int toTen = a % 10;
        if (toTen > 0 && a % 10 < b % 10)
            return "Splits " + b + " in " + toTen + " en " + (b - toTen)
                    + ". Trek eerst " + toTen + " van " + a + " af. Trek daarna de rest af.";
        if (toTen == 0 && b % 10 != 0)
            return "Begin bij " + a + ". Tel " + b + " stappen terug. Gebruik papier als je wilt.";
        return "Splits " + b + " in " + (b / 10 * 10) + " en " + (b % 10)
                + ". Trek eerst de tientallen van " + a + " af. Trek daarna de eenheden af.";
    }
}
