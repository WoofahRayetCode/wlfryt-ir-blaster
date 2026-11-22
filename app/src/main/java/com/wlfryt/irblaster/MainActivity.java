package com.wlfryt.irblaster;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends Activity {
    private static final ScheduledExecutorService IR_WORKER =
            Executors.newSingleThreadScheduledExecutor();

    private final List<Button> buttons = new ArrayList<>();
    private AndroidIrDriver driver;
    private IrSender sender;
    private TextView status;
    private View heldVolume;
    private int heldPointer;
    private boolean finishingVolumeTouch;
    private boolean ready;
    private int requestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View screen = findViewById(R.id.remote_screen);
        screen.setOnApplyWindowInsetsListener(MainActivity::applySystemInsets);

        driver = new AndroidIrDriver(this);
        sender = new IrSender((task, delayMs) -> IR_WORKER.schedule(task, delayMs, TimeUnit.MILLISECONDS),
                SystemClock::elapsedRealtime, driver::transmit);
        status = findViewById(R.id.ir_status);
        bind(R.id.power, VizioProfile.Button.POWER);
        bind(R.id.input, VizioProfile.Button.INPUT);
        bind(R.id.hdmi_1, VizioProfile.Button.HDMI_1);
        bind(R.id.hdmi_2, VizioProfile.Button.HDMI_2);
        bind(R.id.up, VizioProfile.Button.UP);
        bind(R.id.left, VizioProfile.Button.LEFT);
        bind(R.id.ok, VizioProfile.Button.OK);
        bind(R.id.right, VizioProfile.Button.RIGHT);
        bind(R.id.down, VizioProfile.Button.DOWN);
        bind(R.id.back, VizioProfile.Button.BACK);
        bind(R.id.home, VizioProfile.Button.HOME);
        bind(R.id.volume_down, VizioProfile.Button.VOLUME_DOWN);
        bind(R.id.mute, VizioProfile.Button.MUTE);
        bind(R.id.volume_up, VizioProfile.Button.VOLUME_UP);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sender.resume();
        showStatus(R.string.status_checking, false);
        int currentRequest = ++requestId;
        IR_WORKER.execute(() -> {
            AndroidIrDriver.Availability availability = driver.checkAvailability(NecEncoder.CARRIER_HZ);
            runOnUiThread(() -> {
                if (currentRequest == requestId) {
                    showAvailability(availability);
                }
            });
        });
    }

    @Override
    protected void onPause() {
        ++requestId;
        ready = false;
        finishVolumeTouch();
        sender.pause();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            finishVolumeTouch();
        }
    }

    // handleVolumeTouch calls the platform Button's performClick on release.
    @SuppressLint("ClickableViewAccessibility")
    private void bind(int id, VizioProfile.Button command) {
        Button button = findViewById(id);
        buttons.add(button);
        button.setOnClickListener(view -> {
            if (ready && !finishingVolumeTouch) {
                finishVolumeTouch();
                send(command, false);
            }
        });
        if (command.repeatsOnHold()) {
            button.setOnTouchListener((view, event) -> handleVolumeTouch(button, event, command));
            button.setOnFocusChangeListener((view, hasFocus) -> {
                if (!hasFocus && heldVolume == view) {
                    finishVolumeTouch();
                }
            });
        }
    }

    private boolean handleVolumeTouch(Button button, MotionEvent event, VizioProfile.Button command) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (!ready) {
                return false;
            }
            finishVolumeTouch();
            heldVolume = button;
            heldPointer = event.getPointerId(0);
            button.setPressed(true);
            send(command, true);
            return true;
        }
        if (heldVolume != button) {
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> finishVolumeTouch();
            case MotionEvent.ACTION_MOVE -> {
                int pointerIndex = event.findPointerIndex(heldPointer);
                if (pointerIndex == -1 || !stayedInsideButton(button, event, pointerIndex)) {
                    finishVolumeTouch();
                }
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                int pointerIndex = event.getActionIndex();
                if (event.getPointerId(pointerIndex) == heldPointer) {
                    finishVolumeTouch();
                    if (stayedInsideButton(button, event, pointerIndex)) {
                        // Preserve click feedback and accessibility without emitting a second full frame.
                        finishingVolumeTouch = true;
                        try {
                            button.performClick();
                        } finally {
                            finishingVolumeTouch = false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean stayedInsideButton(View button, MotionEvent event, int pointerIndex) {
        for (int index = 0; index < event.getHistorySize(); index++) {
            if (!insideButton(button, event.getHistoricalX(pointerIndex, index),
                    event.getHistoricalY(pointerIndex, index))) {
                return false;
            }
        }
        return insideButton(button, event.getX(pointerIndex), event.getY(pointerIndex));
    }

    private static boolean insideButton(View button, float x, float y) {
        return x >= 0 && y >= 0 && x < button.getWidth() && y < button.getHeight();
    }

    private void finishVolumeTouch() {
        sender.stopHold();
        if (heldVolume != null) {
            heldVolume.setPressed(false);
            heldVolume = null;
        }
    }

    private void send(VizioProfile.Button button, boolean allowRepeat) {
        status.setText(R.string.status_sending);
        int currentRequest = ++requestId;
        sender.send(button, allowRepeat, sent -> runOnUiThread(() -> {
            if (currentRequest == requestId) {
                status.setText(sent ? R.string.status_sent : R.string.status_send_failed);
            }
        }));
    }

    private void showAvailability(AndroidIrDriver.Availability availability) {
        switch (availability) {
            case READY -> showStatus(R.string.status_ready, true);
            case NO_EMITTER -> showStatus(R.string.status_no_emitter, false);
            case NO_SERVICE -> showStatus(R.string.status_no_service, false);
            case CARRIER_UNSUPPORTED -> showStatus(R.string.status_unsupported_carrier, false);
            case QUERY_FAILED -> showStatus(R.string.status_query_failed, false);
        }
    }

    private void showStatus(int message, boolean canSend) {
        ready = canSend;
        status.setText(message);
        for (Button button : buttons) {
            button.setEnabled(canSend);
        }
    }

    @SuppressWarnings("deprecation")
    private static WindowInsets applySystemInsets(View view, WindowInsets windowInsets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Insets insets = windowInsets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsets.CONSUMED;
        }
        view.setPadding(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(),
                windowInsets.getSystemWindowInsetRight(), windowInsets.getSystemWindowInsetBottom());
        return windowInsets.consumeSystemWindowInsets();
    }
}
