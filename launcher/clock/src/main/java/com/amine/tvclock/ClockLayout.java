package com.amine.tvclock;

final class ClockLayout {
    static int position(int value) { return Math.max(0, Math.min(20, value)); }

    static int size(int value) { return Math.max(14, Math.min(40, value)); }

    static int verticalOffset(int screenHeight, int clockHeight, int inset, int position) {
        int available = Math.max(0, screenHeight - clockHeight);
        int margin = Math.min(Math.max(0, inset), available / 2);
        return margin + (available - 2 * margin) * position(position) / 20;
    }
}
