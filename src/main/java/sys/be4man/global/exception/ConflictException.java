package sys.be4man.global.exception;

import org.springframework.http.HttpStatus;
import sys.be4man.global.exception.type.CommonExceptionType;
import sys.be4man.global.exception.type.ExceptionType;

/**
 * 409 CONFLICT 예외
 */
public class ConflictException extends BaseException {
    public ConflictException(final ExceptionType exceptionType) {
        super(exceptionType, HttpStatus.CONFLICT);
    }

    public ConflictException() {
        this(CommonExceptionType.CONFLICT);
    }
}

