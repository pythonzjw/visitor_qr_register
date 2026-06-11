package com.zjw.visitorqrregister;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class LicenseGate {
    private static final String TAG = "LicenseGate";
    private static final String PREFS = "license_state";
    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_LEASE_TOKEN = "lease_token";
    private static final LicenseGate INSTANCE = new LicenseGate();

    private final AtomicBoolean allowed = new AtomicBoolean(false);
    private ScheduledExecutorService executor;
    private LicenseClient client;
    private SharedPreferences prefs;
    private Context appContext;

    private LicenseGate() {}

    static LicenseGate get() { return INSTANCE; }

    synchronized void start(Context context) {
        appContext = context.getApplicationContext();
        if (executor != null && !executor.isShutdown()) return;
        client = new LicenseClient(appContext);
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "license-heartbeat");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(this::verifyOnce, 0L, AppConfig.HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    synchronized void stop() {
        allowed.set(false);
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    boolean isAllowed() { return allowed.get(); }

    private void verifyOnce() {
        try {
            String sessionId = prefs.getString(KEY_SESSION_ID, "");
            String leaseToken = prefs.getString(KEY_LEASE_TOKEN, "");
            LicenseState state = client.verify(sessionId, leaseToken);
            if (state.ok) {
                prefs.edit().putString(KEY_SESSION_ID, state.sessionId).putString(KEY_LEASE_TOKEN, state.leaseToken).apply();
                allowed.set(true);
            } else {
                denySilently(state.reason);
            }
        } catch (Throwable t) {
            Log.d(TAG, "heartbeat failed: " + t.getClass().getSimpleName());
            denySilently("verify_error");
            sleepQuietly(AppConfig.LICENSE_FAIL_RETRY_MS);
        }
    }

    private void denySilently(String reason) {
        allowed.set(false);
        if (appContext != null) {
            RuntimeStore store = new RuntimeStore(appContext);
            store.setRunning(false);
            store.setStatus("已停止");
        }
        if ("device_limit_exceeded".equals(reason) || "project_online_limit_exceeded".equals(reason)) {
            prefs.edit().remove(KEY_SESSION_ID).remove(KEY_LEASE_TOKEN).apply();
        }
    }

    private static void sleepQuietly(long ms) {
        try { Thread.sleep(Math.max(0L, ms)); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
