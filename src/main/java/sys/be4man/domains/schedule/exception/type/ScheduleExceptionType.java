package sys.be4man.domains.schedule.exception.type;

import lombok.RequiredArgsConstructor;
import sys.be4man.global.exception.type.ExceptionType;

/**
 * 스케줄 관리 예외 타입
 */
@RequiredArgsConstructor
public enum ScheduleExceptionType implements ExceptionType {
    BAN_NOT_FOUND("작업 금지 기간을 찾을 수 없습니다"),
    BAN_ALREADY_CANCELED("이미 취소된 작업 금지 기간입니다"),
    INSUFFICIENT_PERMISSION("권한이 부족합니다"),
    INVALID_DATE_RANGE("종료일이 시작일보다 이전입니다"),
    INVALID_TIME_RANGE("종료시간이 시작시간보다 이전입니다"),
    INVALID_DURATION("금지 기간 시간이 올바르지 않습니다"),
    INVALID_RECURRENCE_OPTION("반복 설정이 올바르지 않습니다"),
    START_DATE_REQUIRED("단일 일정은 시작일이 필요합니다"),
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

