package com.opencircle.accountsettings;

import com.opencircle.session.SessionDetails;

import java.util.List;

record AccountSessionsResponse(List<SessionDetails> sessions) {
}
