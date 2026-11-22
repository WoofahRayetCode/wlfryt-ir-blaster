package com.wlfryt.irblaster;

final class VizioProfile {
    // Vizio XRT140R capture from Flipper-IRDB (39c3290b7177f5badb144215897d630531fefb10).
    // https://github.com/Lucaslhm/Flipper-IRDB/blob/39c3290b7177f5badb144215897d630531fefb10/TVs/Vizio/Vizio_XRT140R.ir
    private static final int ADDRESS = 0x04;

    private VizioProfile() {}

    enum Button {
        POWER(0x08),
        INPUT(0x2f),
        VOLUME_UP(0x02),
        VOLUME_DOWN(0x03),
        MUTE(0x09),
        UP(0x45),
        DOWN(0x46),
        LEFT(0x47),
        RIGHT(0x48),
        OK(0x44),
        BACK(0x4a),
        HOME(0x2d);

        private final int command;

        Button(int command) {
            this.command = command;
        }

        int[] encode() {
            return NecEncoder.encode(ADDRESS, command);
        }

        boolean repeatsOnHold() {
            return this == VOLUME_UP || this == VOLUME_DOWN;
        }
    }
}
