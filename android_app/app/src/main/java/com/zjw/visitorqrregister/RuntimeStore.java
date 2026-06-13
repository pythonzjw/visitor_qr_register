package com.zjw.visitorqrregister;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

final class RuntimeStore {
    private static final String PREFS = "visitor_runtime";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_INTERVAL_MINUTES = "interval_minutes";
    private static final String KEY_QR_X_RATIO = "qr_x_ratio";
    private static final String KEY_QR_Y_RATIO = "qr_y_ratio";
    private static final String KEY_NEXT_RUN_AT = "next_run_at";
    private static final String KEY_STATUS = "status";
    private static final String KEY_SUCCESS_COUNT = "success_count";
    private static final String KEY_USED_NAMES = "used_names";
    private static final String KEY_USED_PHONES = "used_phones";
    private static final String KEY_CALIBRATING = "calibrating";
    private final SharedPreferences prefs;

    RuntimeStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    boolean isRunning() { return prefs.getBoolean(KEY_RUNNING, false); }
    void setRunning(boolean running) { prefs.edit().putBoolean(KEY_RUNNING, running).apply(); }

    int intervalMinutes() { return Math.max(1, prefs.getInt(KEY_INTERVAL_MINUTES, AppConfig.DEFAULT_INTERVAL_MINUTES)); }
    void setIntervalMinutes(int minutes) { prefs.edit().putInt(KEY_INTERVAL_MINUTES, Math.max(1, minutes)).apply(); }

    float qrXRatio() { return clamp(prefs.getFloat(KEY_QR_X_RATIO, AppConfig.DEFAULT_QR_X_RATIO)); }
    float qrYRatio() { return clamp(prefs.getFloat(KEY_QR_Y_RATIO, AppConfig.DEFAULT_QR_Y_RATIO)); }
    void setQrRatio(float x, float y) { prefs.edit().putFloat(KEY_QR_X_RATIO, clamp(x)).putFloat(KEY_QR_Y_RATIO, clamp(y)).apply(); }

    boolean isCalibrating() { return prefs.getBoolean(KEY_CALIBRATING, false); }
    void setCalibrating(boolean calibrating) { prefs.edit().putBoolean(KEY_CALIBRATING, calibrating).apply(); }

    long nextRunAt() { return prefs.getLong(KEY_NEXT_RUN_AT, 0L); }
    void scheduleNext(long nowMs) { prefs.edit().putLong(KEY_NEXT_RUN_AT, nowMs + intervalMinutes() * 60_000L).apply(); }
    void runImmediately() { prefs.edit().putLong(KEY_NEXT_RUN_AT, 0L).apply(); }

    String status() { return prefs.getString(KEY_STATUS, "未启动"); }
    void setStatus(String status) { prefs.edit().putString(KEY_STATUS, status == null ? "" : status).apply(); }

    int successCount() { return prefs.getInt(KEY_SUCCESS_COUNT, 0); }

    Set<String> usedNames() { return new HashSet<>(prefs.getStringSet(KEY_USED_NAMES, new HashSet<>())); }
    Set<String> usedPhones() { return new HashSet<>(prefs.getStringSet(KEY_USED_PHONES, new HashSet<>())); }

    void markUsed(String name, String phone) {
        Set<String> names = usedNames();
        Set<String> phones = usedPhones();
        names.add(name);
        phones.add(phone);
        prefs.edit()
                .putStringSet(KEY_USED_NAMES, names)
                .putStringSet(KEY_USED_PHONES, phones)
                .putInt(KEY_SUCCESS_COUNT, successCount() + 1)
                .apply();
    }

    private static float clamp(float value) {
        if (Float.isNaN(value)) return 0.5f;
        return Math.max(0.05f, Math.min(0.95f, value));
    }
}
