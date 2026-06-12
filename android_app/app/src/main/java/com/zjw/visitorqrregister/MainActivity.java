package com.zjw.visitorqrregister;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private RuntimeStore store;
    private EditText intervalInput;
    private EditText qrXInput;
    private EditText qrYInput;
    private TextView statusText;
    private TextView countText;
    private final Handler handler = new Handler();
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            renderRuntime();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new RuntimeStore(this);
        LicenseGate.get().start(this);
        buildUi();
        handleControlIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleControlIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderRuntime();
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("访客登记助手");
        title.setTextSize(22f);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWrap());

        statusText = label("状态：");
        countText = label("成功：0");
        root.addView(statusText, fullWrap());
        root.addView(countText, fullWrap());

        intervalInput = numberInput(String.valueOf(store.intervalMinutes()));
        root.addView(label("轮询间隔（分钟，最小 1）"), fullWrap());
        root.addView(intervalInput, fullWrap());

        qrXInput = decimalInput(String.format(Locale.US, "%.2f", store.qrXRatio()));
        qrYInput = decimalInput(String.format(Locale.US, "%.2f", store.qrYRatio()));
        root.addView(label("二维码长按位置比例 X/Y（0.05~0.95）"), fullWrap());
        root.addView(qrXInput, fullWrap());
        root.addView(qrYInput, fullWrap());

        Button save = button("保存间隔和二维码位置");
        save.setOnClickListener(v -> saveSettings());
        root.addView(save, fullWrapWithTop(12));

        Button access = button("打开无障碍设置");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(access, fullWrapWithTop(12));

        Button openWechat = button("打开微信并校准聊天页");
        openWechat.setOnClickListener(v -> openWechat());
        root.addView(openWechat, fullWrapWithTop(12));

        Button start = button("启动");
        start.setOnClickListener(v -> startAutomation());
        root.addView(start, fullWrapWithTop(20));

        Button stop = button("停止");
        stop.setOnClickListener(v -> stopAutomation());
        root.addView(stop, fullWrapWithTop(12));

        TextView hint = label("使用：先打开微信聊天页，让二维码图片保持可见；按视频位置可先用默认 X=0.71/Y=0.79，点启动后会按间隔执行。授权失败时客户端不显示授权信息并停止。");
        hint.setTextSize(14f);
        root.addView(hint, fullWrapWithTop(20));

        setContentView(root);
    }

    private void handleControlIntent(Intent intent) {
        if (intent == null) return;
        if (intent.getBooleanExtra("adb_stop", false)) {
            stopAutomation();
        }
        if (intent.getBooleanExtra("adb_start", false)) {
            startAutomation();
        }
        if (intent.getBooleanExtra("adb_open_wechat", false)) {
            openWechat();
            renderRuntime();
        }
    }

    private void startAutomation() {
        saveSettings();
        store.setRunning(true);
        store.runImmediately();
        store.setStatus("已启动，等待授权和无障碍调度");
        openWechat();
        renderRuntime();
    }

    private void stopAutomation() {
        store.setRunning(false);
        store.setStatus("已停止");
        renderRuntime();
    }

    private void saveSettings() {
        int minutes = parseInt(intervalInput.getText().toString(), AppConfig.DEFAULT_INTERVAL_MINUTES);
        float x = parseFloat(qrXInput.getText().toString(), AppConfig.DEFAULT_QR_X_RATIO);
        float y = parseFloat(qrYInput.getText().toString(), AppConfig.DEFAULT_QR_Y_RATIO);
        store.setIntervalMinutes(minutes);
        store.setQrRatio(x, y);
        store.setStatus(store.isRunning() ? "已更新设置" : "设置已保存");
        renderRuntime();
    }

    private void renderRuntime() {
        statusText.setText("状态：" + store.status());
        countText.setText("成功提交：" + store.successCount());
    }

    private void openWechat() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            store.setStatus("未找到微信");
        }
    }

    private TextView label(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16f);
        tv.setPadding(0, dp(8), 0, dp(4));
        return tv;
    }

    private EditText numberInput(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        return input;
    }

    private EditText decimalInput(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return input;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    private LinearLayout.LayoutParams fullWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams fullWrapWithTop(int marginDp) {
        LinearLayout.LayoutParams lp = fullWrap();
        lp.topMargin = dp(marginDp);
        return lp;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private static int parseInt(String value, int fallback) {
        try { return Math.max(1, Integer.parseInt(value.trim())); }
        catch (Exception e) { return fallback; }
    }

    private static float parseFloat(String value, float fallback) {
        try { return Float.parseFloat(value.trim()); }
        catch (Exception e) { return fallback; }
    }
}
