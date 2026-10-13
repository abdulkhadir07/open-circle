package com.opencircle.accountsettings;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class AccountDeletionConfirmationException extends ApiException {

    AccountDeletionConfirmationException() {
        super(HttpStatus.BAD_REQUEST, "Type DELETE to confirm account deletion");
    }
}
