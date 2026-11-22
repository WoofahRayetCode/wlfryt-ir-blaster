package com.wlfryt.irblaster;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import static com.wlfryt.irblaster.VizioProfile.Button.INPUT;
import static com.wlfryt.irblaster.VizioProfile.Button.VOLUME_DOWN;
import static com.wlfryt.irblaster.VizioProfile.Button.VOLUME_UP;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class IrSenderTest {
    private record Scheduled(long due, Runnable task) {}

    private final PriorityQueue<Scheduled> queue =
            new PriorityQueue<>(Comparator.comparingLong(Scheduled::due));
    private final List<Long> starts = new ArrayList<>();
    private final List<int[]> patterns = new ArrayList<>();
    private final List<Boolean> results = new ArrayList<>();
    private long now;
    private boolean emitterWorks = true;
    private final IrSender sender = new IrSender(
            (task, delayMs) -> queue.add(new Scheduled(now + delayMs, task)),
            () -> now,
            (carrierHz, pattern) -> {
                assertEquals(38_000, carrierHz);
                starts.add(now);
                patterns.add(pattern);
                now += pattern.length == 3 ? 12 : 68;
                return emitterWorks;
            });

    @Before
    public void resumeRemote() {
        sender.resume();
    }

    @Test
    public void tapReleasedWithAQueuedFullFrameStillTransmitsExactlyOnce() {
        sender.send(VOLUME_UP, true, results::add);
        sender.stopHold();
        runThrough(500);

        assertEquals(List.of(0L), starts);
        assertArrayEquals(VOLUME_UP.encode(), patterns.get(0));
        assertEquals(List.of(true), results);
    }

    @Test
    public void directVolumeClickAndUnrelatedCommandsSendOneFullFrameEach() {
        sender.send(VOLUME_DOWN, false, results::add);
        runThrough(0);
        sender.send(INPUT, true, results::add);
        runThrough(500);

        assertEquals(List.of(0L, 68L), starts);
        assertArrayEquals(VOLUME_DOWN.encode(), patterns.get(0));
        assertArrayEquals(INPUT.encode(), patterns.get(1));
    }

    @Test
    public void holdRepeatsFromThePreviousStartAndReleaseCancelsTheNextPulse() {
        sender.send(VOLUME_UP, true, results::add);
        runThrough(220);
        sender.stopHold();
        runThrough(500);

        assertEquals(List.of(0L, 110L, 220L), starts);
        assertArrayEquals(VOLUME_UP.encode(), patterns.get(0));
        assertArrayEquals(NecEncoder.encodeRepeat(), patterns.get(1));
        assertArrayEquals(NecEncoder.encodeRepeat(), patterns.get(2));
        assertEquals(List.of(true), results);
    }

    @Test
    public void delayedWorkerSchedulesFromTheActualStartWithoutCatchUpBursts() {
        sender.send(VOLUME_UP, true, results::add);
        runThrough(0);
        now = 275;
        runThrough(385);

        assertEquals(List.of(0L, 275L, 385L), starts);
    }

    @Test
    public void switchingVolumeHoldsAndSendingAnUnrelatedCommandCancelOlderRepeats() {
        sender.send(VOLUME_UP, true, results::add);
        runThrough(0);
        sender.send(VOLUME_DOWN, true, results::add);
        runThrough(180);
        sender.send(INPUT, false, results::add);
        runThrough(600);

        assertEquals(List.of(0L, 68L, 178L, 190L), starts);
        assertArrayEquals(VOLUME_UP.encode(), patterns.get(0));
        assertArrayEquals(VOLUME_DOWN.encode(), patterns.get(1));
        assertArrayEquals(NecEncoder.encodeRepeat(), patterns.get(2));
        assertArrayEquals(INPUT.encode(), patterns.get(3));
    }

    @Test
    public void pauseInvalidatesBothQueuedFullFrameAndRepeatAcrossResume() {
        sender.send(VOLUME_UP, true, results::add);
        runThrough(0);
        sender.pause();
        sender.resume();
        runThrough(200);
        sender.send(VOLUME_DOWN, true, results::add);
        sender.pause();
        sender.resume();
        runThrough(500);

        assertEquals(List.of(0L), starts);
    }

    @Test
    public void failedRepeatReportsFailureAndEndsTheHold() {
        sender.send(VOLUME_DOWN, true, results::add);
        runThrough(0);
        emitterWorks = false;
        runThrough(500);

        assertEquals(List.of(0L, 110L), starts);
        assertEquals(List.of(true, false), results);
    }

    private void runThrough(long deadline) {
        while (!queue.isEmpty() && queue.peek().due() <= deadline) {
            Scheduled next = queue.remove();
            now = Math.max(now, next.due());
            next.task().run();
        }
        now = Math.max(now, deadline);
    }
}
