package sys.be4man.domains.taskmanagement.dto;

import lombok.*;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.model.type.DeploymentStage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 작업 관리 페이지 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskManagementResponseDto {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private Long id;                    // 작업 번호
    private String drafter;             // 기안자
    private String department;          // 부서
    private String serviceName;         // 서비스명
    private String taskTitle;           // 작업 제목
    private String stage;               // 처리 단계 (계획서/배포/결과보고)
    private String status;              // 처리 상태
    private String completionTime;      // 완료 시각 (yyyy.MM.dd HH:mm)
    private String result;              // 배포 결과 (성공/실패/null)

    /**
     * ✅ 수정: Deployment 엔티티로부터 DTO 생성
     * - 배포 대기 상태는 "계획서 - 승인완료"로 표시
     */
    /**
     * ✅ 수정: Deployment 엔티티로부터 DTO 생성
     * - 배포 단계에서 IN_PROGRESS/COMPLETED/CANCELED가 아니면 화면에서 숨김
     */
    public TaskManagementResponseDto(Deployment deployment) {
        this.id = deployment.getId();
        this.drafter = deployment.getIssuer() != null ? deployment.getIssuer().getName() : null;
        this.department = deployment.getIssuer() != null && deployment.getIssuer().getDepartment() != null
                ? deployment.getIssuer().getDepartment().getKoreanName() : null;
        this.serviceName = deployment.getProject() != null ? deployment.getProject().getName() : null;
        this.taskTitle = deployment.getTitle();
        this.stage = getDeploymentStageKorean(deployment.getStage());
        this.status = getDeploymentStatusKorean(deployment.getStatus());
        this.completionTime = formatDeploymentCompletionTime(deployment);
        this.result = getDeploymentResult(deployment);
    }
    /**
     * DeploymentStage를 한글로 변환
     */
    private String getDeploymentStageKorean(DeploymentStage stage) {
        if (stage == null) {
            return null;
        }

        switch (stage) {
            case PLAN:
                return "계획서";
            case DEPLOYMENT:
                return "배포";
            case RETRY:
                return "재배포";
            case ROLLBACK:
                return "복구";
            case REPORT:
                return "결과보고";
            default:
                return stage.getKoreanName();
        }
    }

    /**
     * ✅ 수정: DeploymentStatus를 한글로 변환
     */
    private String getDeploymentStatusKorean(DeploymentStatus status) {
        if (status == null) {
            return null;
        }

        switch (status) {
            case PENDING:
                return "대기";
            case APPROVED:
                return "승인";
            case IN_PROGRESS:
                return "진행중";
            case COMPLETED:
                return "완료";
            case REJECTED:
                return "반려";
            case CANCELED:
                return "취소";
            default:
                return status.getKoreanName();
        }
    }

    /**
     * ✅ 수정: Deployment 완료 시각 포맷팅
     * - APPROVED 상태일 때: 승인 완료 시각 표시 (updatedAt)
     * - COMPLETED 상태일 때: 배포 완료 시각 표시 (updatedAt)
     * - REJECTED, CANCELED 상태일 때: 처리 시각 표시 (updatedAt)
     */
    private String formatDeploymentCompletionTime(Deployment deployment) {
        LocalDateTime completionTime = null;

        // 계획서 승인 완료
        if (deployment.getStatus() == DeploymentStatus.APPROVED) {
            completionTime = deployment.getUpdatedAt();
        }
        // 배포 완료
        else if (deployment.getStatus() == DeploymentStatus.COMPLETED) {
            completionTime = deployment.getUpdatedAt();
        }
        // 반려 또는 취소
        else if (deployment.getStatus() == DeploymentStatus.REJECTED ||
                deployment.getStatus() == DeploymentStatus.CANCELED) {
            completionTime = deployment.getUpdatedAt();
        }

        if (completionTime != null) {
            return completionTime.format(DATETIME_FORMATTER);
        }

        return null;
    }

    /**
     * 배포 결과 결정 (성공/실패/null)
     * - COMPLETED 상태이고 isDeployed 값이 있는 경우에만 반환
     */
    private String getDeploymentResult(Deployment deployment) {
        if (deployment.getStatus() == DeploymentStatus.COMPLETED && deployment.getIsDeployed() != null) {
            return deployment.getIsDeployed() ? "성공" : "실패";
        }
        return null;
    }
}