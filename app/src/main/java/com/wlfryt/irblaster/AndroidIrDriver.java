package com.wlfryt.irblaster;

import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.util.Log;

final class AndroidIrDriver {
    private static final String TAG = "AndroidIrDriver";
    private final Context context;

    enum Availability {
        READY, NO_EMITTER, NO_SERVICE, CARRIER_UNSUPPORTED, QUERY_FAILED
    }

    AndroidIrDriver(Context context) {
        this.context = context.getApplicationContext();
    }

    Availability checkAvailability(int carrierHz) {
        try {
            ConsumerIrManager manager = context.getSystemService(ConsumerIrManager.class);
            if (manager == null) {
                return Availability.NO_SERVICE;
            }
            if (!manager.hasIrEmitter()) {
                return Availability.NO_EMITTER;
            }
            ConsumerIrManager.CarrierFrequencyRange[] ranges = manager.getCarrierFrequencies();
            if (ranges == null || ranges.length == 0) {
                return Availability.QUERY_FAILED;
            }
            for (ConsumerIrManager.CarrierFrequencyRange range : ranges) {
                if (range.getMinFrequency() <= carrierHz && carrierHz <= range.getMaxFrequency()) {
                    return Availability.READY;
                }
            }
            return Availability.CARRIER_UNSUPPORTED;
        } catch (RuntimeException exception) {
            Log.w(TAG, "Could not query Android IR capabilities", exception);
            return Availability.QUERY_FAILED;
        }
    }

    boolean transmit(int carrierHz, int[] pattern) {
        try {
            ConsumerIrManager manager = context.getSystemService(ConsumerIrManager.class);
            if (manager == null) {
                return false;
            }
            manager.transmit(carrierHz, pattern);
            return true;
        } catch (RuntimeException exception) {
            Log.w(TAG, "Android IR transmission failed", exception);
            return false;
        }
    }
}
