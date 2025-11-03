package sys.be4man.domains.schedule.dto.response;

import java.util.List;
import lombok.Builder;
import sys.be4man.domains.ban.model.type.BanType;
import sys.be4man.domains.project.model.entity.Project;

/**
 * 스케줄 관리 메타데이터 응답 DTO
 */
public record ScheduleMetadataResponse(
        List<ProjectMetadata> projects,
        List<BanMetadata> banTypes
) {

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
}

