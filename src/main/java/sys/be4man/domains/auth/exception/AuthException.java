// 작성자 : 이원석
package sys.be4man.domains.auth.exception;

import org.springframework.http.HttpStatus;
import sys.be4man.domains.auth.exception.type.AuthExceptionType;
import sys.be4man.global.exception.BaseException;

/**
 * 인증(Auth) 도메인 예외 (401 Unauthorized)
 */
public class AuthException extends BaseException {

    public AuthException(final AuthExceptionType exceptionType) {
        super(exceptionType, HttpStatus.UNAUTHORIZED);
    }
}

