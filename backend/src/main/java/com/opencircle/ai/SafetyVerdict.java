package com.opencircle.ai;

record SafetyVerdict(boolean ok, String reason) {

    static final SafetyVerdict OK = new SafetyVerdict(true, null);

    static SafetyVerdict blocked(String reason) {
        return new SafetyVerdict(false, reason);
    }
}
