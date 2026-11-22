package com.wlfryt.irblaster;

final class NecEncoder {
    static final int CARRIER_HZ = 38_000;
    static final int REPEAT_PERIOD_MS = 110;
    private static final int LEADER_MARK_US = 9_000;
    private static final int MARK_US = 560;
    private static final int ZERO_SPACE_US = 560;
    private static final int ONE_SPACE_US = 1_690;

    private NecEncoder() {}

    static int[] encode(int address, int command) {
        if (((address | command) & ~0xff) != 0) {
            throw new IllegalArgumentException("Standard NEC address and command must be bytes");
        }
        int[] bytes = {address, address ^ 0xff, command, command ^ 0xff};
        int[] pattern = new int[67];
        pattern[0] = LEADER_MARK_US;
        pattern[1] = 4_500;
        int index = 2;
        for (int value : bytes) {
            for (int bit = 0; bit < 8; bit++) {
                pattern[index++] = MARK_US;
                pattern[index++] = ((value >> bit) & 1) == 1 ? ONE_SPACE_US : ZERO_SPACE_US;
            }
        }
        pattern[index] = MARK_US;
        return pattern;
    }

    static int[] encodeRepeat() {
        return new int[] {LEADER_MARK_US, 2_250, MARK_US};
    }
}
