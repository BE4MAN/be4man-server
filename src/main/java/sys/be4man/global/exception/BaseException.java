// 작성자 : 이원석
package sys.be4man.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import sys.be4man.global.exception.type.ExceptionType;

/**
 * 커스텀 예외 기본 클래스 모든 도메인별 예외는 이 클래스를 상속해야 합니다.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final ExceptionType exceptionType;
    private final HttpStatus httpStatus;

    protected BaseException(final ExceptionType exceptionType, final HttpStatus httpStatus) {
        super(exceptionType.getName());  // getName()을 메시지로 사용
        this.exceptionType = exceptionType;
        this.httpStatus = httpStatus;
    }
}
