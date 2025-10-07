package sys.be4man.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import sys.be4man.global.exception.type.ExceptionType;

/**
 * 커스텀 예외
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final HttpStatus httpStatus;

    protected BaseException(final ExceptionType exceptionType, final HttpStatus httpStatus) {
        super(exceptionType.getMessage());
        this.httpStatus = httpStatus;
    }
}
