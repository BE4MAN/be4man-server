package sys.be4man.domains.ban.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.ban.model.type.BanType;
import sys.be4man.domains.ban.model.type.RecurrenceType;
import sys.be4man.domains.ban.model.type.RecurrenceWeekOfMonth;
import sys.be4man.domains.ban.model.type.RecurrenceWeekday;
import sys.be4man.global.model.entity.BaseEntity;

/**
 * 작업 금지 엔티티
 */
@Entity
@Table(name = "ban")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ban extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 52, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BanType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_hours", nullable = false)
    private Integer durationHours;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type")
    private RecurrenceType recurrenceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_weekday")
    private RecurrenceWeekday recurrenceWeekday;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_week_of_month")
    private RecurrenceWeekOfMonth recurrenceWeekOfMonth;

    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;

    @Builder
    public Ban(
            Account account,
            LocalDate startDate,
            LocalTime startTime,
            Integer durationHours,
            LocalDateTime endedAt,
            RecurrenceType recurrenceType,
            RecurrenceWeekday recurrenceWeekday,
            RecurrenceWeekOfMonth recurrenceWeekOfMonth,
            LocalDate recurrenceEndDate,
            String title,
            String description,
            BanType type
    ) {
        this.account = account;
        this.startDate = startDate;
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.durationHours = Objects.requireNonNull(durationHours,
                                                    "durationHours must not be null");
        this.endedAt = endedAt;
        this.recurrenceType = recurrenceType;
        this.recurrenceWeekday = recurrenceWeekday;
        this.recurrenceWeekOfMonth = recurrenceWeekOfMonth;
        this.recurrenceEndDate = recurrenceEndDate;
        this.title = title;
        this.description = description;
        this.type = type;
    }

    /**
     * 금지 기간 업데이트
     */
    public void updatePeriod(LocalDate startDate, LocalTime startTime, Integer durationHours,
            LocalDateTime endedAt) {
        this.startDate = startDate;
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.durationHours = Objects.requireNonNull(durationHours,
                                                    "durationHours must not be null");
        this.endedAt = endedAt;
    }

    /**
     * 반복 설정 업데이트
     */
    public void updateRecurrence(
            RecurrenceType recurrenceType,
            RecurrenceWeekday recurrenceWeekday,
            RecurrenceWeekOfMonth recurrenceWeekOfMonth,
            LocalDate recurrenceEndDate
    ) {
        this.recurrenceType = recurrenceType;
        this.recurrenceWeekday = recurrenceWeekday;
        this.recurrenceWeekOfMonth = recurrenceWeekOfMonth;
        this.recurrenceEndDate = recurrenceEndDate;
    }

    /**
     * 설명 업데이트
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 시작 일시 계산 (단일 일정이 아닌 경우 null 허용)
     */
    public LocalDateTime getStartDateTime() {
        if (startDate == null) {
            return null;
        }
        return LocalDateTime.of(startDate, startTime);
    }

    /**
     * 종료 일시 계산
     */
    public LocalDateTime getComputedEndDateTime() {
        if (endedAt != null) {
            return endedAt;
        }
        LocalDateTime startDateTime = getStartDateTime();
        if (startDateTime == null) {
            return null;
        }
        return startDateTime.plusHours(durationHours);
    }

    /**
     * 기간 검증
     */
    public void validateDurationPositive() {
        if (durationHours == null || durationHours <= 0) {
            throw new IllegalArgumentException("durationHours must be positive");
        }
    }

    /**
     * 시작-종료 간격으로 duration 재계산
     */
    public void recalculateDurationFrom(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Objects.requireNonNull(startDateTime, "startDateTime must not be null");
        Objects.requireNonNull(endDateTime, "endDateTime must not be null");

        long hours = Duration.between(startDateTime, endDateTime).toHours();
        if (hours <= 0) {
            throw new IllegalArgumentException("Duration between start and end must be positive");
        }
        this.startDate = startDateTime.toLocalDate();
        this.startTime = startDateTime.toLocalTime();
        this.durationHours = (int) hours;
        this.endedAt = endDateTime;
    }

    /**
     * 반복 옵션 검증
     */
    public void validateRecurrenceOptions() {
        if (recurrenceType == null) {
            return;
        }

        if (recurrenceType == RecurrenceType.WEEKLY) {
            if (recurrenceWeekday == null) {
                throw new IllegalStateException("Weekly recurrence requires recurrenceWeekday");
            }
        }

        if (recurrenceType == RecurrenceType.MONTHLY) {
            if (recurrenceWeekday == null || recurrenceWeekOfMonth == null) {
                throw new IllegalStateException(
                        "Monthly recurrence requires recurrenceWeekday and recurrenceWeekOfMonth");
            }
        }
    }
}


