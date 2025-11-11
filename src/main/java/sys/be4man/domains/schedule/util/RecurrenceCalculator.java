package sys.be4man.domains.schedule.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import sys.be4man.domains.ban.model.entity.Ban;
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
    public static List<RecurrenceOccurrence> calculateRecurrenceDates(
            Ban ban,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<RecurrenceOccurrence> occurrences = new ArrayList<>();

        if (ban.getRecurrenceType() == null) {
            // 단일 일정
            LocalDateTime banStart = ban.getStartDateTime();
            LocalDateTime banEnd = ban.getComputedEndDateTime();
            LocalDate banStartDate = ban.getStartDate();

            if (banStartDate.isBefore(startDate) || banStartDate.isAfter(endDate)) {
                return occurrences;
            }

            if (banEnd != null && banEnd.toLocalDate().isBefore(startDate)) {
                return occurrences;
            }

            LocalDateTime occurrenceStart = banStart;
            LocalDateTime occurrenceEnd = banEnd != null ? banEnd : banStart.plusHours(ban.getDurationHours());

            if (isOverlapping(occurrenceStart, occurrenceEnd, startDate.atStartOfDay(),
                    endDate.atTime(23, 59, 59))) {
                occurrences.add(new RecurrenceOccurrence(occurrenceStart, occurrenceEnd));
            }
            return occurrences;
        }

        LocalDate recurrenceStartDate = ban.getStartDate();
        LocalDate recurrenceEndDate = ban.getRecurrenceEndDate();
        LocalDate effectiveEndDate = recurrenceEndDate != null
                ? recurrenceEndDate.isBefore(endDate) ? recurrenceEndDate : endDate
                : endDate;

        if (recurrenceStartDate.isAfter(effectiveEndDate)) {
            return occurrences;
        }

        LocalDate queryStartDate = recurrenceStartDate.isBefore(startDate) ? startDate : recurrenceStartDate;
        LocalTime startTime = ban.getStartTime();
        int durationHours = ban.getDurationHours();

        switch (ban.getRecurrenceType()) {
            case DAILY:
                occurrences.addAll(calculateDailyRecurrence(
                        queryStartDate, effectiveEndDate, startTime, durationHours));
                break;
            case WEEKLY:
                if (ban.getRecurrenceWeekday() == null) {
                    break;
                }
                occurrences.addAll(calculateWeeklyRecurrence(
                        queryStartDate, effectiveEndDate, ban.getRecurrenceWeekday(), startTime, durationHours));
                break;
            case MONTHLY:
                if (ban.getRecurrenceWeekday() == null || ban.getRecurrenceWeekOfMonth() == null) {
                    break;
                }
                occurrences.addAll(calculateMonthlyRecurrence(
                        queryStartDate, effectiveEndDate, ban.getRecurrenceWeekOfMonth(),
                        ban.getRecurrenceWeekday(), startTime, durationHours));
                break;
        }

        return occurrences;
    }

    /**
     * 일일 반복 계산
     */
    private static List<RecurrenceOccurrence> calculateDailyRecurrence(
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            int durationHours
    ) {
        List<RecurrenceOccurrence> occurrences = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            LocalDateTime occurrenceStart = LocalDateTime.of(current, startTime);
            LocalDateTime occurrenceEnd = occurrenceStart.plusHours(durationHours);
            occurrences.add(new RecurrenceOccurrence(occurrenceStart, occurrenceEnd));
            current = current.plusDays(1);
        }

        return occurrences;
    }

    /**
     * 주간 반복 계산
     */
    private static List<RecurrenceOccurrence> calculateWeeklyRecurrence(
            LocalDate startDate,
            LocalDate endDate,
            RecurrenceWeekday weekday,
            LocalTime startTime,
            int durationHours
    ) {
        List<RecurrenceOccurrence> occurrences = new ArrayList<>();
        DayOfWeek targetDayOfWeek = mapToDayOfWeek(weekday);

        LocalDate current = startDate;
        LocalDate firstOccurrence = current.with(TemporalAdjusters.nextOrSame(targetDayOfWeek));

        if (firstOccurrence.isAfter(endDate)) {
            return occurrences;
        }

        LocalDate occurrenceDate = firstOccurrence;
        while (!occurrenceDate.isAfter(endDate)) {
            LocalDateTime occurrenceStart = LocalDateTime.of(occurrenceDate, startTime);
            LocalDateTime occurrenceEnd = occurrenceStart.plusHours(durationHours);
            occurrences.add(new RecurrenceOccurrence(occurrenceStart, occurrenceEnd));
            occurrenceDate = occurrenceDate.plusWeeks(1);
        }

        return occurrences;
    }

    /**
     * 월간 반복 계산 (N번째 주의 요일)
     */
    private static List<RecurrenceOccurrence> calculateMonthlyRecurrence(
            LocalDate startDate,
            LocalDate endDate,
            RecurrenceWeekOfMonth weekOfMonth,
            RecurrenceWeekday weekday,
            LocalTime startTime,
            int durationHours
    ) {
        List<RecurrenceOccurrence> occurrences = new ArrayList<>();
        DayOfWeek targetDayOfWeek = mapToDayOfWeek(weekday);

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            LocalDate occurrenceDate = findNthWeekdayOfMonth(current, weekOfMonth, targetDayOfWeek);

            if (occurrenceDate != null && !occurrenceDate.isBefore(startDate) && !occurrenceDate.isAfter(endDate)) {
                LocalDateTime occurrenceStart = LocalDateTime.of(occurrenceDate, startTime);
                LocalDateTime occurrenceEnd = occurrenceStart.plusHours(durationHours);
                occurrences.add(new RecurrenceOccurrence(occurrenceStart, occurrenceEnd));
            }

            current = current.plusMonths(1).withDayOfMonth(1);
        }

        return occurrences;
    }

    /**
     * 특정 월의 N번째 주의 요일 찾기
     *
     * @param month      대상 월의 임의 날짜
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
    public static class RecurrenceOccurrence {
        private final LocalDateTime startDateTime;
        private final LocalDateTime endDateTime;

        public RecurrenceOccurrence(LocalDateTime startDateTime, LocalDateTime endDateTime) {
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;
        }

        public LocalDateTime getStartDateTime() {
            return startDateTime;
        }

        public LocalDateTime getEndDateTime() {
            return endDateTime;
        }
    }
}

