package com.opencircle.session;

public enum SessionRevocationReason {
    LOGOUT,
    LOGOUT_ALL,
    PASSWORD_RESET,
    PASSWORD_CHANGE,
    EMAIL_CHANGE,
    USER_REVOKED,
    REFRESH_TOKEN_REUSE
}
