package sys.be4man.domains.taskmanagement.dto;

import lombok.*;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.model.type.DeploymentStage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 작업 관리 페이지 응답 DTO
 * - 프론트엔드 LogManagement 컴포넌트에 맞춘 데이터 구조
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskManagementResponseDto {

    private Long id;                        // 작업 번호
    private String drafter;                 // 기안자
    private String department;              // 부서
    private String serviceName;             // 서비스명
    private String taskTitle;               // 작업 제목
    private String stage;                   // 처리 단계 (계획서/배포/결과보고)
    private String status;                  // 처리 상태 (승인대기/반려/진행중/취소/완료)
    private String completionTime;          // 완료 시각 (YYYY.MM.DD HH:mm)
    private String result;                  // 배포 결과 (성공/실패/null)

    /**
     * Deployment 엔티티로부터 DTO 생성
     */
    public TaskManagementResponseDto(Deployment deployment) {
        this.id = deployment.getId();
        this.drafter = getDrafterName(deployment);
        this.department = getDepartmentName(deployment);
        this.serviceName = getServiceName(deployment);
        this.taskTitle = deployment.getTitle();

        // 처리 단계 매핑 (DeploymentStage 사용)
        this.stage = deployment.getStage() != null
            ? deployment.getStage().getKoreanName()
            : determineStageFromStatus(deployment.getStatus());

        // 처리 상태 매핑 (DeploymentStatus의 koreanName 직접 사용)
        this.status = deployment.getStatus() != null
            ? deployment.getStatus().getKoreanName()
            : null;

        // 완료 시각 포맷팅
        this.completionTime = formatCompletionTime(deployment);

        // 배포 결과 매핑
        this.result = determineDeploymentResult(deployment.getStatus(), deployment.getIsDeployed());
    }

    /**
     * 기안자 이름 조회
     */
    private String getDrafterName(Deployment deployment) {
        return deployment.getIssuer() != null ? deployment.getIssuer().getName() : null;
    }

    /**
     * 부서명 조회
     */
    private String getDepartmentName(Deployment deployment) {
        if (deployment.getIssuer() != null && deployment.getIssuer().getDepartment() != null) {
            return deployment.getIssuer().getDepartment().getKoreanName();
        }
        return null;
    }

    /**
     * 서비스명 조회
     */
    private String getServiceName(Deployment deployment) {
        return deployment.getProject() != null ? deployment.getProject().getName() : null;
    }

    /**
     * DeploymentStatus로부터 처리 단계 결정
     * - 계획서: stage가 PLAN이고 PENDING만
     * - 배포: stage가 DEPLOYMENT이고 (IN_PROGRESS, COMPLETED, CANCELED)
     * - 결과보고: stage가 REPORT이고 (PENDING, REJECTED, APPROVED)
     *
     * stage가 없는 경우 status로 추론 (하위 호환성)
     */
    private String determineStageFromStatus(DeploymentStatus deploymentStatus) {
        if (deploymentStatus == null) {
            return null;
        }

        // stage가 없는 경우 status로 추론
        switch (deploymentStatus) {
            case PENDING:
                return "계획서"; // 또는 "결과보고" (stage로 구분 필요)

            case IN_PROGRESS:
            case COMPLETED:
            case CANCELED:
                return "배포";

            case REJECTED:
            case APPROVED:
                return "결과보고";

            default:
                return null;
        }
    }

    /**
     * 완료 시각 포맷팅 (YYYY.MM.DD HH:mm)
     */
    private String formatCompletionTime(Deployment deployment) {
        LocalDateTime completionTime = null;

        // 배포 완료 시각 또는 업데이트 시각 사용
        if (deployment.getStatus() == DeploymentStatus.COMPLETED) {
            completionTime = deployment.getUpdatedAt();
        } else if (deployment.getScheduledToEndedAt() != null) {
            completionTime = deployment.getScheduledToEndedAt();
        }

        if (completionTime != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
            return completionTime.format(formatter);
        }

        return null;
    }

    /**
     * 배포 결과 결정 (성공/실패/null)
     */
    private String determineDeploymentResult(DeploymentStatus deploymentStatus, Boolean isDeployed) {
        if (deploymentStatus == null) {
            return null;
        }

        // COMPLETED 상태이고 isDeployed 값이 있는 경우
        if (deploymentStatus == DeploymentStatus.COMPLETED && isDeployed != null) {
            return isDeployed ? "성공" : "실패";
        }

        return null;
    }
}
