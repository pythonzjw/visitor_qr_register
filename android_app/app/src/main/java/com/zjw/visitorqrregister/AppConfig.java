package com.zjw.visitorqrregister;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class AppConfig {
    static final String PROJECT_KEY = "visitor_qr_register";
    static final String LICENSE_VERIFY_URL = "http://47.116.98.81:5003/api/verify";
    static final long HEARTBEAT_INTERVAL_MS = 30L * 60L * 1000L;
    static final long LICENSE_FAIL_RETRY_MS = 60_000L;
    static final long AUTOMATION_POLL_MS = 5_000L;
    static final long ROUND_TIMEOUT_MS = 90_000L;
    static final int DEFAULT_INTERVAL_MINUTES = 5;
    static final float DEFAULT_QR_X_RATIO = 0.71f;
    static final float DEFAULT_QR_Y_RATIO = 0.79f;

    static final String[] QR_MENU_TEXTS = {"识别图中二维码", "识别二维码", "识别图片中的二维码", "识别图中的二维码"};
    static final String[] MINI_PROGRAM_MARKERS = {"访客通行", "访客登记", "来访社区"};
    static final String[] PURPOSE_TEXTS = {"外卖快递"};
    static final String[] SUBMIT_TEXTS = {"提交审核", "提交"};
    static final String[] SUCCESS_TEXTS = {"请至门岗登记核实", "重新申请"};
    static final String[] CLOSE_DIALOG_TEXTS = {"取消", "关闭", "我知道了"};

    // 由 android_app/scripts/configure_license_center.py 自动写入。
    static final Map<String, String> RESPONSE_PUBLIC_KEYS = buildResponsePublicKeys();

    private AppConfig() {}

    private static Map<String, String> buildResponsePublicKeys() {
        Map<String, String> keys = new HashMap<>();
        keys.put("visitor_qr_register_v1",
                "-----BEGIN PUBLIC KEY-----
"
                        + "MCowBQYDK2VwAyEAJsKZ/G/z1HuY1JahRjj+zYaxOmSXXMM49X2RCVLbU74=
"
                        + "-----END PUBLIC KEY-----
");
        return Collections.unmodifiableMap(keys);
    }
}
