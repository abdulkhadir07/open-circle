package com.opencircle.invitepost;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
class InvitePostTag {

    @Column(name = "tag", nullable = false, length = InvitePost.MAX_TAG_LENGTH)
    private String value;

    @Column(name = "display_order", nullable = false)
    private Short displayOrder;

    protected InvitePostTag() {
    }

    InvitePostTag(String value, short displayOrder) {
        this.value = value;
        this.displayOrder = displayOrder;
    }

    String getValue() {
        return value;
    }
}
