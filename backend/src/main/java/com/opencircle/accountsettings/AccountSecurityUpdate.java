package com.opencircle.accountsettings;

import com.opencircle.session.IssuedSession;

record AccountSecurityUpdate(
        String accessToken,
        IssuedSession session
) {
}
