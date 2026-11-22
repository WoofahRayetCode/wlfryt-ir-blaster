package com.wlfryt.irblaster;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class VizioProfileTest {
    @Test
    public void volumeUpUsesVizioVolumeCommand() {
        assertArrayEquals(new int[] {0x04, 0xfb, 0x02, 0xfd},
                NecEncoderTest.decodeBytes(VizioProfile.Button.VOLUME_UP.encode()));
    }

    @Test
    public void inputUsesVizioInputCommand() {
        assertArrayEquals(new int[] {0x04, 0xfb, 0x2f, 0xd0},
                NecEncoderTest.decodeBytes(VizioProfile.Button.INPUT.encode()));
    }

    @Test
    public void dailyRemoteUsesTheVizioAddressAndCapturedCommands() {
        assertCommand(VizioProfile.Button.POWER, 0x08);
        assertCommand(VizioProfile.Button.VOLUME_DOWN, 0x03);
        assertCommand(VizioProfile.Button.MUTE, 0x09);
        assertCommand(VizioProfile.Button.UP, 0x45);
        assertCommand(VizioProfile.Button.DOWN, 0x46);
        assertCommand(VizioProfile.Button.LEFT, 0x47);
        assertCommand(VizioProfile.Button.RIGHT, 0x48);
        assertCommand(VizioProfile.Button.OK, 0x44);
        assertCommand(VizioProfile.Button.BACK, 0x4a);
        assertCommand(VizioProfile.Button.HOME, 0x2d);
        assertEquals(12, VizioProfile.Button.values().length);
    }

    private static void assertCommand(VizioProfile.Button button, int command) {
        assertArrayEquals(new int[] {0x04, 0xfb, command, command ^ 0xff},
                NecEncoderTest.decodeBytes(button.encode()));
    }
}
