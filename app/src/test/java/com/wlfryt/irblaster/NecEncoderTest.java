package com.wlfryt.irblaster;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class NecEncoderTest {
    @Test
    public void encodesAddressCommandAndInversesLeastSignificantBitFirst() {
        assertArrayEquals(new int[] {0x04, 0xfb, 0x02, 0xfd},
                decodeBytes(NecEncoder.encode(0x04, 0x02)));
        assertArrayEquals(new int[] {0x04, 0xfb, 0x2f, 0xd0},
                decodeBytes(NecEncoder.encode(0x04, 0x2f)));
    }

    @Test
    public void emitsOneFrameWithNecPulseTimingsAndStopMark() {
        int[] pattern = NecEncoder.encode(0x04, 0x02);
        assertEquals(38_000, NecEncoder.CARRIER_HZ);
        assertEquals(67, pattern.length);
        assertEquals(9_000, pattern[0]);
        assertEquals(4_500, pattern[1]);
        assertEquals(560, pattern[3]);
        assertEquals(1_690, pattern[7]);
        for (int index = 2; index < pattern.length; index += 2) {
            assertEquals(560, pattern[index]);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAnAddressBeyondTheStandardNecByte() {
        NecEncoder.encode(0x100, 0x02);
    }

    @Test
    public void emitsTheNecRepeatPulseWithoutCommandData() {
        assertArrayEquals(new int[] {9_000, 2_250, 560}, NecEncoder.encodeRepeat());
    }

    static int[] decodeBytes(int[] pattern) {
        int[] bytes = new int[4];
        for (int index = 0; index < 32; index++) {
            int space = pattern[3 + 2 * index];
            if (space == 1_690) {
                bytes[index / 8] |= 1 << (index % 8);
            } else {
                assertEquals(560, space);
            }
        }
        return bytes;
    }
}
