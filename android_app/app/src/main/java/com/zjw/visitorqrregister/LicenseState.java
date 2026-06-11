package com.zjw.visitorqrregister;

final class LicenseState {
    final boolean ok;
    final String reason;
    final String sessionId;
    final String leaseToken;
    final long leaseExpiresAtMs;

    LicenseState(boolean ok, String reason, String sessionId, String leaseToken, long leaseExpiresAtMs) {
        this.ok = ok;
        this.reason = reason == null ? "" : reason;
        this.sessionId = sessionId == null ? "" : sessionId;
        this.leaseToken = leaseToken == null ? "" : leaseToken;
        this.leaseExpiresAtMs = leaseExpiresAtMs;
    }

    static LicenseState denied(String reason) {
        return new LicenseState(false, reason, "", "", 0L);
    }
}
