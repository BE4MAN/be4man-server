package sys.be4man.global.exception;

import org.springframework.http.HttpStatus;
import sys.be4man.global.exception.type.CommonExceptionType;
import sys.be4man.global.exception.type.ExceptionType;

/**
 * 400 BAD REQUEST 예외
 */
public class BadRequestException extends BaseException {

    public BadRequestException(final ExceptionType exceptionType) {
        super(exceptionType, HttpStatus.BAD_REQUEST);
    }

    public BadRequestException() {
        this(CommonExceptionType.BAD_REQUEST);
    }
}
