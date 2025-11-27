// 작성자 : 이원석
package sys.be4man.domains.auth.exception.type;

import lombok.RequiredArgsConstructor;
import sys.be4man.global.exception.type.ExceptionType;

/**
 * 인증(Auth) 도메인 예외 타입 (401 Unauthorized)
 */
@RequiredArgsConstructor
public enum AuthExceptionType implements ExceptionType {

    // SignToken 관련
    INVALID_SIGN_TOKEN("유효하지 않은 SignToken입니다"),
    EXPIRED_SIGN_TOKEN("만료된 SignToken입니다"),
    SIGN_TOKEN_INFO_NOT_FOUND("SignToken 정보를 찾을 수 없습니다"),

    // Access Token 관련
    INVALID_ACCESS_TOKEN("유효하지 않은 Access Token입니다"),
    ACCESS_TOKEN_PARSE_FAILED("Access Token 파싱에 실패했습니다"),

    // Refresh Token 관련
    REFRESH_TOKEN_NOT_FOUND("Refresh Token을 찾을 수 없습니다"),
    INVALID_REFRESH_TOKEN("유효하지 않은 Refresh Token입니다"),
    EXPIRED_REFRESH_TOKEN("만료된 Refresh Token입니다"),
    REFRESH_TOKEN_MISMATCH("Refresh Token이 일치하지 않습니다"),

    // OAuth Temporary Code 관련
    INVALID_TEMP_CODE("유효하지 않은 임시 코드입니다"),
    TEMP_CODE_EXPIRED("임시 코드가 만료되었습니다");

    private final String message;

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }
}

