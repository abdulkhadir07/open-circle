package com.opencircle.banter;

enum BanterSort {
    NEW,
    HOT;

    static BanterSort parse(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("new")) {
            return NEW;
        }

        if (value.equalsIgnoreCase("hot")) {
            return HOT;
        }

        throw new InvalidBanterRequestException("Sort must be 'new' or 'hot'");
    }
}
