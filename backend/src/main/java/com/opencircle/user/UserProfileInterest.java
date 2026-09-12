package com.opencircle.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
class UserProfileInterest {

    @Column(name = "interest", nullable = false, length = UserProfile.MAX_INTEREST_LENGTH)
    private String value;

    @Column(name = "display_order", nullable = false)
    private Short displayOrder;

    protected UserProfileInterest() {
    }

    UserProfileInterest(String value, short displayOrder) {
        this.value = value;
        this.displayOrder = displayOrder;
    }

    String getValue() {
        return value;
    }
}
