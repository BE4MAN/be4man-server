package sys.be4man.domains.schedule.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import sys.be4man.domains.ban.model.type.BanType;
import sys.be4man.domains.ban.model.type.RecurrenceType;
import sys.be4man.domains.ban.model.type.RecurrenceWeekOfMonth;
import sys.be4man.domains.ban.model.type.RecurrenceWeekday;

/**
 * 작업 금지 기간 생성 요청 DTO
 */
public record CreateBanRequest(
        @NotBlank(message = "제목은 필수입니다")
        String title,

        @NotBlank(message = "설명은 필수입니다")
        String description,

        @NotNull(message = "시작일은 필수입니다")
        LocalDate startDate,

        @NotNull(message = "시작시간은 필수입니다")
        LocalTime startTime,

        @NotNull(message = "금지 시간(duration)은 필수입니다")
        @Positive(message = "금지 시간(duration)은 0보다 커야 합니다")
        Integer durationHours,

        LocalDateTime endedAt,

        @NotNull(message = "작업 금지 유형은 필수입니다")
        BanType type,

        @NotEmpty(message = "연관 프로젝트는 최소 1개 이상 필요합니다")
        List<Long> relatedProjectIds,

        RecurrenceType recurrenceType,

        RecurrenceWeekday recurrenceWeekday,

        RecurrenceWeekOfMonth recurrenceWeekOfMonth,

        LocalDate recurrenceEndDate
) {


    @AssertTrue(message = "주간 반복은 요일(recurrenceWeekday)이 필요합니다")
    public boolean isWeeklyRecurrenceValid() {
        if (recurrenceType == RecurrenceType.WEEKLY) {
            return recurrenceWeekday != null;
        }
        return true;
    }

    @AssertTrue(message = "월간 반복은 반복 주차/요일(recurrenceWeekOfMonth, recurrenceWeekday)이 모두 필요합니다")
    public boolean isMonthlyRecurrenceValid() {
        if (recurrenceType == RecurrenceType.MONTHLY) {
            return recurrenceWeekday != null && recurrenceWeekOfMonth != null;
        }
        return true;
    }

    @AssertTrue(message = "종료 시각(endedAt)은 시작 시각보다 빠를 수 없습니다")
    public boolean isEndAfterStart() {
        if (startDate == null || endedAt == null) {
            return true;
        }
        LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
        return !endedAt.isBefore(startDateTime);
    }
}
