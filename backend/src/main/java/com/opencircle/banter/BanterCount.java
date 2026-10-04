package com.opencircle.banter;

import java.util.UUID;

// One row of a grouped count query, so a whole page of counts costs one query.
interface BanterCount {

    UUID getBanterId();

    long getTotal();
}
