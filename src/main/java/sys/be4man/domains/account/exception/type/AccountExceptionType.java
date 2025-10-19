package sys.be4man.domains.account.exception.type;

import lombok.RequiredArgsConstructor;
import sys.be4man.global.exception.type.ExceptionType;

/**
 * 계정(Account) 도메인 예외 타입
 */
@RequiredArgsConstructor
public enum AccountExceptionType implements ExceptionType {

    ACCOUNT_NOT_FOUND("계정을 찾을 수 없습니다");

    private final String message;

    @Override
    public String getMessage() {
        return message;
    }
}

