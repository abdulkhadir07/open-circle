package com.opencircle.score;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class AnnualAwardNotFinalizedException extends ApiException {

    AnnualAwardNotFinalizedException(int seasonYear) {
        super(HttpStatus.NOT_FOUND, "Annual award has not been finalized for " + seasonYear);
    }
}
