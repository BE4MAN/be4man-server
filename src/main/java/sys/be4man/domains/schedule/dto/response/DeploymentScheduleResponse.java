// 작성자 : 이원석
package sys.be4man.domains.schedule.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.Builder;
import sys.be4man.domains.deployment.model.entity.Deployment;

/**
 * 배포 작업 스케줄 응답 DTO
 */
@Builder
public record DeploymentScheduleResponse(
        Long id,
        String title,
        String status,
        String stage,
        Boolean isDeployed,
        String projectName,
        List<String> relatedServices,
        LocalDate scheduledDate,
        LocalTime scheduledTime,
        String registrant,
        String registrantDepartment
) {

    /**
     * Deployment 엔티티로부터 DeploymentScheduleResponse 생성 (하위 호환성)
     * - status: DeploymentStatus enum 이름 (PENDING, REJECTED, IN_PROGRESS, CANCELED, COMPLETED, APPROVED)
     * - stage: DeploymentStage enum 이름 (PLAN, DEPLOYMENT, REPORT, etc.)
     * - isDeployed: Jenkins 배포 성공 여부 (null: 배포 전, true: 성공, false: 실패)
     * - relatedServices: 빈 리스트로 설정
     */
    public static DeploymentScheduleResponse from(Deployment deployment) {
        return from(deployment, List.of());
    }

    /**
     * Deployment 엔티티와 관련 서비스 목록으로부터 DeploymentScheduleResponse 생성
     * - status: DeploymentStatus enum 이름 (PENDING, REJECTED, IN_PROGRESS, CANCELED, COMPLETED, APPROVED)
     * - stage: DeploymentStage enum 이름 (PLAN, DEPLOYMENT, REPORT, etc.)
     * - isDeployed: Jenkins 배포 성공 여부 (null: 배포 전, true: 성공, false: 실패)
     * - relatedServices: 관련 서비스(프로젝트) 이름 목록
     */
    public static DeploymentScheduleResponse from(Deployment deployment, List<String> relatedServices) {
        return from(deployment, relatedServices, deployment.getIsDeployed());
    }

    /**
     * Deployment 엔티티와 관련 서비스 목록, isDeployed 값으로부터 DeploymentScheduleResponse 생성
     * - status: DeploymentStatus enum 이름 (PENDING, REJECTED, IN_PROGRESS, CANCELED, COMPLETED, APPROVED)
     * - stage: DeploymentStage enum 이름 (PLAN, DEPLOYMENT, REPORT, etc.)
     * - isDeployed: 계산된 isDeployed 값 (stage와 status 기반)
     * - relatedServices: 관련 서비스(프로젝트) 이름 목록
     */
    public static DeploymentScheduleResponse from(Deployment deployment, List<String> relatedServices, Boolean isDeployed) {
        String registrant = deployment.getIssuer() != null ? deployment.getIssuer().getName() : null;
        String registrantDepartment = (deployment.getIssuer() != null
                && deployment.getIssuer().getDepartment() != null)
                ? deployment.getIssuer().getDepartment().name()
                : null;

        return DeploymentScheduleResponse.builder()
                .id(deployment.getId())
                .title(deployment.getTitle())
                .status(deployment.getStatus().name())
                .stage(deployment.getStage().name())
                .isDeployed(isDeployed)
                .projectName(deployment.getProject().getName())
                .relatedServices(relatedServices)
                .scheduledDate(deployment.getScheduledAt().toLocalDate())
                .scheduledTime(deployment.getScheduledAt().toLocalTime())
                .registrant(registrant)
                .registrantDepartment(registrantDepartment)
                .build();
    }
}

