package sys.be4man.domains.schedule.exception.type;

import lombok.RequiredArgsConstructor;
import sys.be4man.global.exception.type.ExceptionType;

/**
 * 스케줄 관리 예외 타입
 */
@RequiredArgsConstructor
public enum ScheduleExceptionType implements ExceptionType {
    BAN_NOT_FOUND("작업 금지 기간을 찾을 수 없습니다"),
    INVALID_DATE_RANGE("종료일이 시작일보다 이전입니다"),
    INVALID_TIME_RANGE("종료시간이 시작시간보다 이전입니다"),
    PROJECT_NOT_FOUND("프로젝트를 찾을 수 없습니다"),
    DUPLICATE_PROJECT_IDS("중복된 프로젝트 ID가 있습니다"),
    EMPTY_PROJECT_IDS("프로젝트 ID 목록이 비어있습니다");

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

