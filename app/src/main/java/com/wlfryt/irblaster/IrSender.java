package com.wlfryt.irblaster;

final class IrSender {
    interface Scheduler {
        void schedule(Runnable task, long delayMs);
    }

    interface Clock {
        long nowMs();
    }

    interface Emitter {
        boolean transmit(int carrierHz, int[] pattern);
    }

    interface ResultListener {
        void onResult(boolean sent);
    }

    private final Scheduler worker;
    private final Clock clock;
    private final Emitter emitter;
    private volatile Object activeSession;
    private volatile Object hold;

    IrSender(Scheduler worker, Clock clock, Emitter emitter) {
        this.worker = worker;
        this.clock = clock;
        this.emitter = emitter;
    }

    // Commands and lifecycle changes enter on the UI thread. All pulses run on the serial worker.
    void resume() {
        activeSession = new Object();
    }

    void pause() {
        activeSession = null;
        stopHold();
    }

    void stopHold() {
        hold = null;
    }

    void send(VizioProfile.Button button, boolean allowRepeat, ResultListener listener) {
        Object session = activeSession;
        if (session == null) {
            return;
        }
        Object currentHold = allowRepeat && button.repeatsOnHold() ? new Object() : null;
        hold = currentHold;
        worker.schedule(() -> {
            int[] pattern = button.encode();
            if (activeSession != session) {
                return;
            }
            long started = clock.nowMs();
            boolean sent = emitter.transmit(NecEncoder.CARRIER_HZ, pattern);
            listener.onResult(sent);
            if (sent && currentHold != null) {
                scheduleRepeat(session, currentHold, started, listener);
            }
        }, 0);
    }

    private void scheduleRepeat(Object session, Object currentHold, long lastStart,
            ResultListener listener) {
        if (activeSession != session || hold != currentHold) {
            return;
        }
        long delay = Math.max(0, lastStart + NecEncoder.REPEAT_PERIOD_MS - clock.nowMs());
        worker.schedule(() -> {
            if (activeSession != session || hold != currentHold) {
                return;
            }
            long started = clock.nowMs();
            if (emitter.transmit(NecEncoder.CARRIER_HZ, NecEncoder.encodeRepeat())) {
                scheduleRepeat(session, currentHold, started, listener);
            } else {
                listener.onResult(false);
            }
        }, delay);
    }
}
