package sys.be4man.global.exception;

import org.springframework.http.HttpStatus;
import sys.be4man.global.exception.type.CommonExceptionType;
import sys.be4man.global.exception.type.ExceptionType;

/**
 * 403 FORBIDDEN 예외
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(final ExceptionType exceptionType) {
        super(exceptionType, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException() {
        this(CommonExceptionType.FORBIDDEN);
    }

}

