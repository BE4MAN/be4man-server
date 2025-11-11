package sys.be4man.domains.schedule.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import sys.be4man.domains.ban.model.entity.Ban;

/**
 * 작업 금지 기간 응답 DTO
 */
public record BanResponse(
        String id,
        String title,
        String description,
        String startDate,
        String startTime,
        String endedAt,
        Integer durationHours,
        String type,
        List<String> services,
        String registrant,
        String registrantDepartment,
        String recurrenceType,
        String recurrenceWeekday,
        String recurrenceWeekOfMonth,
        String recurrenceEndDate
) {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Ban 엔티티와 연관 프로젝트 이름 목록으로부터 BanResponse 생성
     */
    public static BanResponse from(Ban ban, List<String> services) {
        return from(ban, services, ban.getStartDate(), ban.getComputedEndDateTime());
    }

    /**
     * Ban 엔티티와 실제 발생 일정으로부터 BanResponse 생성 (반복 Ban용)
     */
    public static BanResponse from(Ban ban, List<String> services, LocalDate occurrenceDate,
            LocalDateTime occurrenceEndDateTime) {
        String startDate = occurrenceDate.toString();
        String startTime = ban.getStartTime().format(TIME_FORMATTER);
        String endedAt = occurrenceEndDateTime != null ? occurrenceEndDateTime.toString() : null;

        String recurrenceType = ban.getRecurrenceType() != null ? ban.getRecurrenceType().name() : null;
        String recurrenceWeekday = ban.getRecurrenceWeekday() != null
                ? ban.getRecurrenceWeekday().name()
                : null;
        String recurrenceWeekOfMonth = ban.getRecurrenceWeekOfMonth() != null
                ? ban.getRecurrenceWeekOfMonth().name()
                : null;
        String recurrenceEndDate = ban.getRecurrenceEndDate() != null
                ? ban.getRecurrenceEndDate().toString()
                : null;

        String registrant = ban.getAccount() != null ? ban.getAccount().getName() : null;
        String registrantDepartment = (ban.getAccount() != null && ban.getAccount().getDepartment() != null)
                ? ban.getAccount().getDepartment().name()
                : null;

        return new BanResponse(
                ban.getId().toString(),
                ban.getTitle(),
                ban.getDescription(),
                startDate,
                startTime,
                endedAt,
                ban.getDurationHours(),
                ban.getType().name(),
                services,
                registrant,
                registrantDepartment,
                recurrenceType,
                recurrenceWeekday,
                recurrenceWeekOfMonth,
                recurrenceEndDate
        );
    }
}
