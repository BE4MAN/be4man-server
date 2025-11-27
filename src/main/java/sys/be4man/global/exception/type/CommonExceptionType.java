// 작성자 : 이원석
package sys.be4man.global.exception.type;

import lombok.RequiredArgsConstructor;

/**
 * 공통 예외 타입
 */
@RequiredArgsConstructor
public enum CommonExceptionType implements ExceptionType {
    BAD_REQUEST("잘못된 요청입니다"),
    UNAUTHORIZED("인증이 필요합니다"),
    FORBIDDEN("접근 권한이 없습니다"),
    NOT_FOUND("해당 자원을 찾을 수 없습니다"),
    CONFLICT("요청이 충돌합니다");

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
