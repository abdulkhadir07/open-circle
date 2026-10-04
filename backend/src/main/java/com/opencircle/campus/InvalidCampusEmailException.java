package com.opencircle.campus;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCampusEmailException extends ApiException {

    public InvalidCampusEmailException(String requiredSuffix) {
        super(HttpStatus.BAD_REQUEST, "Use your school email address (it must end in " + requiredSuffix + ")");
    }
}
