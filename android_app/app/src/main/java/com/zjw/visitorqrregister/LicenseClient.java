package com.zjw.visitorqrregister;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

final class LicenseClient {
    private static final String TAG = "LicenseClient";
    private final String machineId;

    LicenseClient(Context context) {
        this.machineId = DeviceIdentity.machineId(context.getApplicationContext());
    }

    LicenseState verify(String sessionId, String leaseToken) {
        HttpURLConnection conn = null;
        try {
            JSONObject req = new JSONObject();
            req.put("project_key", AppConfig.PROJECT_KEY);
            req.put("machine_id", machineId);
            req.put("device_fingerprint", new JSONObject(DeviceIdentity.fingerprintJson()));
            if (sessionId != null && !sessionId.isEmpty()) req.put("session_id", sessionId);
            if (leaseToken != null && !leaseToken.isEmpty()) req.put("lease_token", leaseToken);

            byte[] payload = req.toString().getBytes(StandardCharsets.UTF_8);
            conn = (HttpURLConnection) new URL(AppConfig.LICENSE_VERIFY_URL).openConnection();
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) { out.write(payload); }

            int code = conn.getResponseCode();
            InputStream in = code >= 200 && code < 400 ? conn.getInputStream() : conn.getErrorStream();
            JSONObject res = new JSONObject(readAll(in));
            if (!verifyResponseSignature(res)) return LicenseState.denied("invalid_response_signature");
            if (!AppConfig.PROJECT_KEY.equals(res.optString("project_key", ""))) return LicenseState.denied("project_mismatch");
            if (!machineId.equals(res.optString("machine_id", ""))) return LicenseState.denied("machine_mismatch");
            if (!res.optBoolean("ok", false)) return LicenseState.denied(res.optString("reason", "denied"));

            String nextSessionId = res.optString("session_id", sessionId == null ? "" : sessionId);
            String nextLeaseToken = res.optString("lease_token", leaseToken == null ? "" : leaseToken);
            long leaseExpiresAtMs = parseIsoMillis(res.optString("lease_expires_at", ""));
            return new LicenseState(true, res.optString("reason", "authorized"), nextSessionId, nextLeaseToken, leaseExpiresAtMs);
        } catch (Exception e) {
            Log.d(TAG, "verify failed: " + e.getClass().getSimpleName());
            return LicenseState.denied("verify_error");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private boolean verifyResponseSignature(JSONObject res) throws Exception {
        String keyId = res.optString("response_key_id", "");
        String algorithm = res.optString("response_algorithm", "");
        String signatureB64 = res.optString("response_signature", "");
        String pem = AppConfig.RESPONSE_PUBLIC_KEYS.get(keyId);
        if (!"Ed25519".equals(algorithm) || keyId.isEmpty() || signatureB64.isEmpty() || pem == null || pem.isEmpty()) return false;
        PublicKey key = parsePublicKey(pem);
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(key);
        verifier.update(canonicalResponsePayload(res).getBytes(StandardCharsets.UTF_8));
        return verifier.verify(Base64.getDecoder().decode(signatureB64));
    }

    private static PublicKey parsePublicKey(String pem) throws Exception {
        String body = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s+", "");
        return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(body)));
    }

    private static String canonicalResponsePayload(JSONObject res) throws Exception {
        JSONObject copy = new JSONObject(res.toString());
        copy.remove("response_signature");
        return canonicalJson(copy);
    }

    private static String canonicalJson(Object value) throws Exception {
        if (value == null || value == JSONObject.NULL) return "null";
        if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            List<String> keys = new ArrayList<>();
            Iterator<String> it = obj.keys();
            while (it.hasNext()) keys.add(it.next());
            Collections.sort(keys);
            StringBuilder sb = new StringBuilder().append('{');
            for (int i = 0; i < keys.size(); i++) {
                if (i > 0) sb.append(',');
                String key = keys.get(i);
                sb.append(DeviceIdentity.jsonString(key)).append(':').append(canonicalJson(obj.get(key)));
            }
            return sb.append('}').toString();
        }
        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            StringBuilder sb = new StringBuilder().append('[');
            for (int i = 0; i < arr.length(); i++) {
                if (i > 0) sb.append(',');
                sb.append(canonicalJson(arr.get(i)));
            }
            return sb.append(']').toString();
        }
        if (value instanceof String) return DeviceIdentity.jsonString((String) value);
        if (value instanceof Boolean || value instanceof Number) return String.valueOf(value);
        return DeviceIdentity.jsonString(String.valueOf(value));
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static long parseIsoMillis(String value) {
        if (value == null || value.isEmpty()) return 0L;
        try { return OffsetDateTime.parse(value).toInstant().toEpochMilli(); }
        catch (DateTimeParseException e) { return 0L; }
    }
}
