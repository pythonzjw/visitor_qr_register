package com.zjw.visitorqrregister;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class VisitorAccessibilityService extends AccessibilityService {
    private static final String TAG = "VisitorService";
    private RuntimeStore store;
    private WindowManager windowManager;
    private View calibrationOverlay;
    private final Handler handler = new Handler();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "visitor-automation");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean executing = new AtomicBoolean(false);
    private final IdentityGenerator generator = new IdentityGenerator();
    private final Runnable scheduler = new Runnable() {
        @Override public void run() {
            syncCalibrationOverlay();
            tick();
            handler.postDelayed(this, AppConfig.AUTOMATION_POLL_MS);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    | AccessibilityEvent.TYPE_VIEW_SCROLLED
                    | AccessibilityEvent.TYPE_VIEW_CLICKED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.notificationTimeout = 50;
            info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            setServiceInfo(info);
        }
        store = new RuntimeStore(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LicenseGate.get().start(this);
        handler.post(scheduler);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        syncCalibrationOverlay();
    }
    @Override public void onInterrupt() {}

    @Override
    public void onDestroy() {
        handler.removeCallbacks(scheduler);
        hideCalibrationOverlay();
        executor.shutdownNow();
        super.onDestroy();
    }

    private void tick() {
        if (store == null || !store.isRunning()) return;
        if (!LicenseGate.get().isAllowed()) {
            store.setRunning(false);
            store.setStatus("已停止");
            return;
        }
        long now = System.currentTimeMillis();
        if (store.nextRunAt() > now || !executing.compareAndSet(false, true)) return;
        executor.execute(() -> {
            try {
                runRound();
            } catch (Throwable t) {
                String reason = t.getMessage() == null || t.getMessage().isEmpty()
                        ? t.getClass().getSimpleName()
                        : t.getMessage();
                Log.d(TAG, "round failed: " + reason);
                store.setStatus("本轮失败：" + reason);
            } finally {
                store.scheduleNext(System.currentTimeMillis());
                executing.set(false);
            }
        });
    }

    private void runRound() throws Exception {
        long deadline = SystemClock.uptimeMillis() + AppConfig.ROUND_TIMEOUT_MS;
        store.setStatus("打开微信");
        openWechat();
        sleep(2500);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int x = Math.round(dm.widthPixels * store.qrXRatio());
        int y = Math.round(dm.heightPixels * store.qrYRatio());

        store.setStatus("打开二维码图片");
        tap(x, y);
        sleep(1000);

        int previewX = Math.round(dm.widthPixels * 0.5f);
        int previewY = Math.round(dm.heightPixels * 0.45f);
        store.setStatus("长按大图二维码");
        longPress(previewX, previewY);
        sleep(1200);

        store.setStatus("识别二维码");
        if (!clickAnyText(AppConfig.QR_MENU_TEXTS)) throw new IllegalStateException("qr_menu_not_found");

        store.setStatus("等待小程序");
        if (!waitForAny(AppConfig.MINI_PROGRAM_MARKERS, deadline)) throw new IllegalStateException("mini_program_timeout");
        closeKnownDialog();
        sleep(600);

        store.setStatus("选择来访目的");
        clickAnyText(AppConfig.PURPOSE_TEXTS);
        sleep(600);

        IdentityGenerator.Identity identity = generator.next(store.usedNames(), store.usedPhones());
        store.setStatus("填写资料");
        if (!fillEditableFields(identity.name, identity.phone)) throw new IllegalStateException("fields_not_editable");
        sleep(600);

        store.setStatus("提交审核");
        if (!clickAnyText(AppConfig.SUBMIT_TEXTS)) throw new IllegalStateException("submit_not_found");
        if (!waitForAny(AppConfig.SUCCESS_TEXTS, deadline)) throw new IllegalStateException("submit_timeout");
        store.markUsed(identity.name, identity.phone);
        store.setStatus("提交成功");
    }

    private void completeCalibration(float rawX, float rawY) {
        if (store == null) return;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int tapX = Math.round(rawX);
        int tapY = Math.round(rawY);
        float x = rawX / (float) Math.max(1, dm.widthPixels);
        float y = rawY / (float) Math.max(1, dm.heightPixels);
        store.setQrRatio(x, y);
        store.setCalibrating(false);
        store.setStatus(String.format(Locale.US, "校准完成：X=%.2f Y=%.2f", store.qrXRatio(), store.qrYRatio()));
        hideCalibrationOverlay();
        Toast.makeText(this, "二维码位置校准完成", Toast.LENGTH_SHORT).show();
        handler.postDelayed(() -> tap(tapX, tapY), 150L);
        handler.postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK), 1200L);
    }

    private void openWechat() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private boolean waitForAny(String[] markers, long deadline) throws InterruptedException {
        while (SystemClock.uptimeMillis() < deadline) {
            closeKnownDialog();
            String text = pageText();
            for (String marker : markers) {
                if (marker != null && !marker.isEmpty() && text.contains(marker)) return true;
            }
            sleep(800);
        }
        return false;
    }

    private void closeKnownDialog() {
        String text = pageText();
        if (text.contains("过期") || text.contains("温馨提示") || text.contains("我知道了")) {
            clickAnyText(AppConfig.CLOSE_DIALOG_TEXTS);
        }
    }

    private boolean clickAnyText(String[] texts) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<NodeRef> nodes = new ArrayList<>();
        collect(root, nodes);
        for (String target : texts) {
            if (target == null || target.isEmpty()) continue;
            for (NodeRef ref : nodes) {
                if (ref.label.contains(target)) {
                    clickNodeOrTap(ref.node, ref.bounds.centerX(), ref.bounds.centerY());
                    return true;
                }
            }
        }
        return false;
    }

    private boolean fillEditableFields(String name, String phone) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<NodeRef> editables = new ArrayList<>();
        collectEditable(root, editables);
        editables.sort(Comparator.comparingInt(a -> a.bounds.top));

        List<NodeRef> candidates = new ArrayList<>();
        for (NodeRef ref : editables) {
            if (ref.bounds.height() < 20 || ref.bounds.width() < 80) continue;
            String label = ref.label == null ? "" : ref.label;
            if (label.contains("搜索") || label.contains("评论")) continue;
            candidates.add(ref);
        }
        if (candidates.size() < 2) return false;
        boolean nameOk = setNodeText(candidates.get(0).node, name);
        sleepQuiet(300);
        boolean phoneOk = setNodeText(candidates.get(1).node, phone);
        return nameOk && phoneOk;
    }

    private boolean setNodeText(AccessibilityNodeInfo node, String value) {
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
        if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return true;
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    private String pageText() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "";
        List<NodeRef> nodes = new ArrayList<>();
        collect(root, nodes);
        StringBuilder sb = new StringBuilder();
        for (NodeRef ref : nodes) sb.append(ref.label).append('\n');
        return sb.toString();
    }

    private void collect(AccessibilityNodeInfo node, List<NodeRef> out) {
        if (node == null) return;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String label = ((text == null ? "" : text.toString()) + " " + (desc == null ? "" : desc.toString())).trim();
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (!label.isEmpty() && !bounds.isEmpty()) out.add(new NodeRef(node, label, bounds));
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChild(i), out);
    }

    private void collectEditable(AccessibilityNodeInfo node, List<NodeRef> out) {
        if (node == null) return;
        CharSequence cls = node.getClassName();
        boolean editable = node.isEditable() || (cls != null && cls.toString().contains("EditText"));
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (editable && !bounds.isEmpty()) {
            CharSequence text = node.getText();
            CharSequence desc = node.getContentDescription();
            String label = ((text == null ? "" : text.toString()) + " " + (desc == null ? "" : desc.toString())).trim();
            out.add(new NodeRef(node, label, bounds));
        }
        for (int i = 0; i < node.getChildCount(); i++) collectEditable(node.getChild(i), out);
    }

    private void clickNodeOrTap(AccessibilityNodeInfo node, int x, int y) {
        AccessibilityNodeInfo cur = node;
        while (cur != null) {
            if (cur.isClickable() && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return;
            cur = cur.getParent();
        }
        tap(x, y);
    }

    private void tap(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0L, 60L))
                .build();
        dispatchGesture(gesture, null, null);
    }

    private void longPress(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0L, 850L))
                .build();
        dispatchGesture(gesture, null, null);
    }

    private void sleep(long ms) throws InterruptedException { Thread.sleep(ms); }
    private void sleepQuiet(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    private void syncCalibrationOverlay() {
        if (store == null) return;
        if (store.isCalibrating()) {
            showCalibrationOverlay();
        } else {
            hideCalibrationOverlay();
        }
    }

    private void showCalibrationOverlay() {
        if (calibrationOverlay != null || windowManager == null) return;
        View overlay = new View(this);
        overlay.setBackgroundColor(0x00000000);
        overlay.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                completeCalibration(event.getRawX(), event.getRawY());
            }
            return true;
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        try {
            windowManager.addView(overlay, params);
            calibrationOverlay = overlay;
            store.setStatus("校准中：请点击二维码图片位置");
        } catch (Throwable t) {
            Log.d(TAG, "show calibration overlay failed", t);
            store.setCalibrating(false);
            store.setStatus("校准失败：请确认无障碍已开启");
        }
    }

    private void hideCalibrationOverlay() {
        if (calibrationOverlay == null || windowManager == null) return;
        try {
            windowManager.removeView(calibrationOverlay);
        } catch (Throwable t) {
            Log.d(TAG, "hide calibration overlay failed", t);
        } finally {
            calibrationOverlay = null;
        }
    }

    private static final class NodeRef {
        final AccessibilityNodeInfo node;
        final String label;
        final Rect bounds;
        NodeRef(AccessibilityNodeInfo node, String label, Rect bounds) {
            this.node = node;
            this.label = label;
            this.bounds = bounds;
        }
    }
}
