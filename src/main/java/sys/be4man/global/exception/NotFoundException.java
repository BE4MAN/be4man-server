package sys.be4man.global.exception;

import org.springframework.http.HttpStatus;
import sys.be4man.global.exception.type.CommonExceptionType;
import sys.be4man.global.exception.type.ExceptionType;

/**
 * 404 NOT FOUND 예외
 */
public class NotFoundException extends BaseException {
    public NotFoundException(final ExceptionType exceptionType) {
        super(exceptionType, HttpStatus.NOT_FOUND);
    }

    public NotFoundException() {
        this(CommonExceptionType.NOT_FOUND);
    }
}
