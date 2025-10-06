package sys.be4man.global.exception;

import org.springframework.http.HttpStatus;
import sys.be4man.global.exception.type.CommonExceptionType;
import sys.be4man.global.exception.type.ExceptionType;

/**
 * 401 UNAUTHORIZED 예외
 */
public class UnauthorizedException extends BaseException {
    public UnauthorizedException(final ExceptionType exceptionType) {
        super(exceptionType, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException() {
        this(CommonExceptionType.UNAUTHORIZED);
    }
}

