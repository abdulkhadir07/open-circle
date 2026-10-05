package com.opencircle.ai;

public enum ContentKind {
    INVITE("invite post"),
    MESSAGE("chat message"),
    BANTER("banter post");

    private final String label;

    ContentKind(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }
}
