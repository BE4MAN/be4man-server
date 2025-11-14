package sys.be4man.domains.schedule.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import sys.be4man.domains.ban.model.entity.Ban;
import sys.be4man.domains.ban.model.type.RecurrenceType;
import sys.be4man.domains.ban.model.type.RecurrenceWeekOfMonth;
import sys.be4man.domains.ban.model.type.RecurrenceWeekday;

/**
 * 반복 일정 계산 유틸리티
 */
public class RecurrenceCalculator {

    /**
     * Ban의 반복 일정을 날짜 범위 내에서 실제 발생 일정 리스트로 변환
     *
     * @param ban       반복 Ban 엔티티
     * @param startDate 조회 시작일 (포함)
     * @param endDate   조회 종료일 (포함)
     * @return 실제 발생 일정 리스트 (각각의 startDateTime, endDateTime 포함)
     */
    public static List<Period> calculateRecurrenceDates(
            Ban ban,
            LocalDate startDate,
            LocalDate endDate
    ) {
        LocalDateTime endedAt = ban.getEndedAt();
        return calculateRecurrenceDates(
                ban.getRecurrenceType(),
                ban.getStartDate(),
                ban.getStartTime(),
                ban.getDurationMinutes(),
                endedAt,
                ban.getRecurrenceWeekday(),
                ban.getRecurrenceWeekOfMonth(),
                ban.getRecurrenceEndDate(),
                startDate,
                endDate
        );
    }

    /**
     * 반복 일정을 날짜 범위 내에서 실제 발생 일정 리스트로 변환 (Ban 객체 없이)
     *
     * @param recurrenceType        반복 유형 (null이면 단일 일정)
     * @param startDate             시작일
     * @param startTime             시작 시간
     * @param durationMinutes       지속 시간 (분)
     * @param endedAt               종료 일시 (null이면 startDate + startTime + durationMinutes로 계산)
     * @param recurrenceWeekday     반복 요일 (WEEKLY, MONTHLY일 때 필수)
     * @param recurrenceWeekOfMonth 반복 주차 (MONTHLY일 때 필수)
     * @param recurrenceEndDate     반복 종료일 (null이면 무한 반복)
     * @param queryStartDate        조회 시작일 (포함)
     * @param queryEndDate          조회 종료일 (포함)
     * @return 실제 발생 일정 리스트 (각각의 startDateTime, endDateTime 포함)
     */
    public static List<Period> calculateRecurrenceDates(
            RecurrenceType recurrenceType,
            LocalDate startDate,
            LocalTime startTime,
            Integer durationMinutes,
            LocalDateTime endedAt,
            RecurrenceWeekday recurrenceWeekday,
            RecurrenceWeekOfMonth recurrenceWeekOfMonth,
            LocalDate recurrenceEndDate,
            LocalDate queryStartDate,
            LocalDate queryEndDate
    ) {
        List<Period> occurrences = new ArrayList<>();

        if (recurrenceType == null) {
            // 단일 일정
            LocalDateTime banStart = LocalDateTime.of(startDate, startTime);
            LocalDateTime banEnd = endedAt != null ? endedAt : banStart.plusMinutes(durationMinutes);

            if (startDate.isBefore(queryStartDate) || startDate.isAfter(queryEndDate)) {
                return occurrences;
            }

            if (banEnd.toLocalDate().isBefore(queryStartDate)) {
                return occurrences;
            }

            if (isOverlapping(banStart, banEnd, queryStartDate.atStartOfDay(),
                              queryEndDate.atTime(23, 59, 59))) {
                occurrences.add(new Period(banStart, banEnd));
            }
            return occurrences;
        }

        LocalDate effectiveEndDate = recurrenceEndDate != null
                ? recurrenceEndDate.isBefore(queryEndDate) ? recurrenceEndDate : queryEndDate
                : queryEndDate;

        if (startDate.isAfter(effectiveEndDate)) {
            return occurrences;
        }

        LocalDate queryStart = startDate.isBefore(queryStartDate) ? queryStartDate : startDate;

        switch (recurrenceType) {
            case DAILY:
                occurrences.addAll(calculateDailyRecurrence(
                        queryStart, effectiveEndDate, startTime, durationMinutes));
                break;
            case WEEKLY:
                if (recurrenceWeekday == null) {
                    break;
                }
                occurrences.addAll(calculateWeeklyRecurrence(
                        queryStart, effectiveEndDate, recurrenceWeekday, startTime, durationMinutes));
                break;
            case MONTHLY:
                if (recurrenceWeekday == null || recurrenceWeekOfMonth == null) {
                    break;
                }
                occurrences.addAll(calculateMonthlyRecurrence(
                        queryStart, effectiveEndDate, recurrenceWeekOfMonth,
                        recurrenceWeekday, startTime, durationMinutes));
                break;
        }

        return occurrences;
    }

    /**
     * 일일 반복 계산
     */
    private static List<Period> calculateDailyRecurrence(
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            int durationMinutes
    ) {
        List<Period> occurrences = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            LocalDateTime occurrenceStart = LocalDateTime.of(current, startTime);
            LocalDateTime occurrenceEnd = occurrenceStart.plusMinutes(durationMinutes);
            occurrences.add(new Period(occurrenceStart, occurrenceEnd));
            current = current.plusDays(1);
        }

        return occurrences;
    }

    /**
     * 주간 반복 계산
     */
    private static List<Period> calculateWeeklyRecurrence(
            LocalDate startDate,
            LocalDate endDate,
            RecurrenceWeekday weekday,
            LocalTime startTime,
            int durationMinutes
    ) {
        List<Period> occurrences = new ArrayList<>();
        DayOfWeek targetDayOfWeek = mapToDayOfWeek(weekday);

        LocalDate firstOccurrence = startDate.with(TemporalAdjusters.nextOrSame(targetDayOfWeek));

        if (firstOccurrence.isAfter(endDate)) {
            return occurrences;
        }

        LocalDate occurrenceDate = firstOccurrence;
        while (!occurrenceDate.isAfter(endDate)) {
            LocalDateTime occurrenceStart = LocalDateTime.of(occurrenceDate, startTime);
            LocalDateTime occurrenceEnd = occurrenceStart.plusMinutes(durationMinutes);
            occurrences.add(new Period(occurrenceStart, occurrenceEnd));
            occurrenceDate = occurrenceDate.plusWeeks(1);
        }

        return occurrences;
    }

    /**
     * 월간 반복 계산 (N번째 주의 요일)
     */
    private static List<Period> calculateMonthlyRecurrence(
            LocalDate startDate,
            LocalDate endDate,
            RecurrenceWeekOfMonth weekOfMonth,
            RecurrenceWeekday weekday,
            LocalTime startTime,
            int durationMinutes
    ) {
        List<Period> occurrences = new ArrayList<>();
        DayOfWeek targetDayOfWeek = mapToDayOfWeek(weekday);

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            LocalDate occurrenceDate = findNthWeekdayOfMonth(current, weekOfMonth, targetDayOfWeek);

            if (occurrenceDate != null && !occurrenceDate.isBefore(startDate)
                    && !occurrenceDate.isAfter(endDate)) {
                LocalDateTime occurrenceStart = LocalDateTime.of(occurrenceDate, startTime);
                LocalDateTime occurrenceEnd = occurrenceStart.plusMinutes(durationMinutes);
                occurrences.add(new Period(occurrenceStart, occurrenceEnd));
            }

            current = current.plusMonths(1).withDayOfMonth(1);
        }

        return occurrences;
    }

    /**
     * 특정 월의 N번째 주의 요일 찾기
     *
     * @param month       대상 월의 임의 날짜
     * @param weekOfMonth N번째 주 (FIRST, SECOND, THIRD, FOURTH, FIFTH)
     * @param dayOfWeek   요일
     * @return 해당 날짜, 없으면 null (예: 5번째 주가 없는 경우)
     */
    private static LocalDate findNthWeekdayOfMonth(
            LocalDate month,
            RecurrenceWeekOfMonth weekOfMonth,
            DayOfWeek dayOfWeek
    ) {
        LocalDate firstDayOfMonth = month.withDayOfMonth(1);
        LocalDate firstOccurrence = firstDayOfMonth.with(TemporalAdjusters.firstInMonth(dayOfWeek));

        int weekIndex = weekOfMonth.ordinal();

        LocalDate nthOccurrence = firstOccurrence.plusWeeks(weekIndex);

        if (nthOccurrence.getMonth() != firstDayOfMonth.getMonth()) {
            return null;
        }

        return nthOccurrence;
    }

    /**
     * RecurrenceWeekday를 DayOfWeek로 변환
     */
    private static DayOfWeek mapToDayOfWeek(RecurrenceWeekday weekday) {
        return switch (weekday) {
            case MON -> DayOfWeek.MONDAY;
            case TUE -> DayOfWeek.TUESDAY;
            case WED -> DayOfWeek.WEDNESDAY;
            case THU -> DayOfWeek.THURSDAY;
            case FRI -> DayOfWeek.FRIDAY;
            case SAT -> DayOfWeek.SATURDAY;
            case SUN -> DayOfWeek.SUNDAY;
        };
    }

    /**
     * 두 시간 범위가 겹치는지 확인
     */
    private static boolean isOverlapping(
            LocalDateTime start1,
            LocalDateTime end1,
            LocalDateTime start2,
            LocalDateTime end2
    ) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * 반복 일정의 실제 발생 일정을 나타내는 클래스
     */
    public record Period(LocalDateTime startDateTime, LocalDateTime endDateTime) {

    }
}

