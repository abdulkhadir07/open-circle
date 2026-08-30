package com.opencircle.location;

import java.math.BigDecimal;

interface LocationResolver {

    ResolvedLocation resolve(BigDecimal latitude, BigDecimal longitude);
}
