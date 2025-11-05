package sys.be4man.domains.schedule.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatusForScheduleMapper;

/**
 * 배포 작업 스케줄 응답 DTO
 */
@Builder
public record DeploymentScheduleResponse(
        Long id,
        String title,
        String status,
        String projectName,
        String prTitle,
        String prBranch,
        LocalDate scheduledDate,
        LocalTime scheduledTime
) {

    /**
     * Deployment 엔티티로부터 DeploymentScheduleResponse 생성 DeploymentStage와 DeploymentStatus 조합으로 상태 매핑
     */
    public static DeploymentScheduleResponse from(Deployment deployment) {
        String status = DeploymentStatusForScheduleMapper.map(
                deployment.getStage(),
                deployment.getStatus(),
                deployment.getIsDeployed()
        );

        return DeploymentScheduleResponse.builder()
                .id(deployment.getId())
                .title(deployment.getTitle())
                .status(status)
                .projectName(deployment.getProject().getName())
                .prTitle(deployment.getPullRequest().getTitle())
                .prBranch(deployment.getPullRequest().getBranch())
                .scheduledDate(deployment.getScheduledAt().toLocalDate())
                .scheduledTime(deployment.getScheduledAt().toLocalTime())
                .build();
    }
}

