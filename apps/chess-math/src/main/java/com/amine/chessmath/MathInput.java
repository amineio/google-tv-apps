package com.amine.chessmath;

/** Remote keypad entry, with no keyboard or Android dependency. */
public final class MathInput {
    private MathInput() { }

    public static String append(String input, int digit) {
        if (digit < 0 || digit > 9) throw new IllegalArgumentException("Ongeldig cijfer.");
        String current = normalize(input);
        if (current.length() >= 3) return current;
        int value = current.isEmpty() ? digit : Integer.parseInt(current) * 10 + digit;
        return value <= 100 ? Integer.toString(value) : current;
    }

    public static String delete(String input) {
        String current = normalize(input);
        return current.isEmpty() ? "" : current.substring(0, current.length() - 1);
    }

    public static boolean matches(String input, Exercise exercise) {
        try {
            String value = normalize(input);
            return !value.isEmpty() && exercise != null && Integer.parseInt(value) == exercise.answer();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String normalize(String input) {
        if (input == null || input.length() > 3) throw new IllegalArgumentException("Ongeldig antwoord.");
        if (input.isEmpty()) return "";
        for (int i = 0; i < input.length(); i++)
            if (input.charAt(i) < '0' || input.charAt(i) > '9')
                throw new IllegalArgumentException("Gebruik alleen cijfers.");
        int value = Integer.parseInt(input);
        if (value > 100) throw new IllegalArgumentException("Antwoord hoger dan 100.");
        return Integer.toString(value);
    }
}
