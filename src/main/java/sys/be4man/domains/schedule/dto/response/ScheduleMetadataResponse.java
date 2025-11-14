package sys.be4man.domains.schedule.dto.response;

import java.util.List;
import java.util.stream.Stream;
import lombok.Builder;
import sys.be4man.domains.ban.model.type.BanType;
import sys.be4man.domains.ban.model.type.RecurrenceType;
import sys.be4man.domains.ban.model.type.RecurrenceWeekOfMonth;
import sys.be4man.domains.ban.model.type.RecurrenceWeekday;
import sys.be4man.domains.project.model.entity.Project;

/**
 * 스케줄 관리 메타데이터 응답 DTO
 */
public record ScheduleMetadataResponse(
        List<ProjectMetadata> projects,
        List<BanMetadata> restrictedPeriodTypes,
        List<EnumMetadata> recurrenceTypes,
        List<EnumMetadata> recurrenceWeekdays,
        List<EnumMetadata> recurrenceWeeksOfMonth
) {

    /**
     * Project 목록으로부터 ScheduleMetadataResponse 생성
     */
    public static ScheduleMetadataResponse from(List<Project> projects) {
        return new ScheduleMetadataResponse(
                projects.stream().map(ProjectMetadata::from).toList(),
                Stream.of(BanType.values()).map(BanMetadata::from).toList(),
                EnumMetadata.fromRecurrenceTypes(),
                EnumMetadata.fromRecurrenceWeekdays(),
                EnumMetadata.fromRecurrenceWeeksOfMonth()
        );
    }

    /**
     * 프로젝트 메타데이터
     */
    @Builder
    public record ProjectMetadata(
            Long id,
            String name
    ) {

        /**
         * Project 엔티티로부터 ProjectMetadata 생성
         */
        public static ProjectMetadata from(Project project) {
            return ProjectMetadata.builder()
                    .id(project.getId())
                    .name(project.getName())
                    .build();
        }
    }

    /**
     * 작업 금지 유형 메타데이터
     */
    @Builder
    public record BanMetadata(
            String value,
            String label
    ) {

        /**
         * BanType enum으로부터 BanMetadata 생성
         */
        public static BanMetadata from(BanType banType) {
            return BanMetadata.builder()
                    .value(banType.name())
                    .label(banType.getKoreanName())
                    .build();
        }
    }

    /**
     * Enum 메타데이터 (반복 관련)
     */
    @Builder
    public record EnumMetadata(
            String value,
            String label
    ) {

        /**
         * RecurrenceType enum으로부터 EnumMetadata 목록 생성
         */
        public static List<EnumMetadata> fromRecurrenceTypes() {
            return Stream.of(RecurrenceType.values())
                    .map(type -> EnumMetadata.builder()
                            .value(type.name())
                            .label(type.getKoreanName())
                            .build())
                    .toList();
        }

        /**
         * RecurrenceWeekday enum으로부터 EnumMetadata 목록 생성
         */
        public static List<EnumMetadata> fromRecurrenceWeekdays() {
            return Stream.of(RecurrenceWeekday.values())
                    .map(weekday -> EnumMetadata.builder()
                            .value(weekday.name())
                            .label(weekday.getKoreanName())
                            .build())
                    .toList();
        }

        /**
         * RecurrenceWeekOfMonth enum으로부터 EnumMetadata 목록 생성
         */
        public static List<EnumMetadata> fromRecurrenceWeeksOfMonth() {
            return Stream.of(RecurrenceWeekOfMonth.values())
                    .map(weekOfMonth -> EnumMetadata.builder()
                            .value(weekOfMonth.name())
                            .label(weekOfMonth.getKoreanName())
                            .build())
                    .toList();
        }
    }
}

