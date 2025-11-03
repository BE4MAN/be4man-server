package sys.be4man.domains.taskmanagement.dto;

import lombok.*;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.common.model.type.ProcessingStage;
import sys.be4man.domains.common.model.type.ProcessingStatus;
import sys.be4man.domains.common.model.type.ReportStatus;

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

        // 처리 단계 매핑
        ProcessingStage processingStage = determineProcessingStage(
            deployment.getStatus(),
            deployment.getReportStatus()
        );
        this.stage = processingStage != null ? processingStage.getKoreanName() : null;

        // 처리 상태 매핑
        ProcessingStatus processingStatus = determineProcessingStatus(
            deployment.getStatus(),
            deployment.getReportStatus(),
            processingStage
        );
        this.status = processingStatus != null ? processingStatus.getKoreanName() : null;

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
     * 처리 단계 결정
     * - 계획서: STAGED, PENDING, APPROVED, REJECTED, CANCELED
     * - 배포: DEPLOYMENT
     * - 결과보고: COMPLETED (ReportStatus 있음)
     */
    private ProcessingStage determineProcessingStage(
        DeploymentStatus deploymentStatus,
        ReportStatus reportStatus
    ) {
        if (deploymentStatus == null) {
            return null;
        }

        switch (deploymentStatus) {
            case STAGED:
            case PENDING:
            case APPROVED:
            case REJECTED:
            case CANCELED:
                return ProcessingStage.PLAN;

            case DEPLOYMENT:
                return ProcessingStage.DEPLOYMENT;

            case COMPLETED:
                // 완료 후 보고서 상태가 있으면 결과보고, 없으면 배포
                return reportStatus != null ? ProcessingStage.REPORT : ProcessingStage.DEPLOYMENT;

            default:
                return null;
        }
    }

    /**
     * 처리 상태 결정
     */
    private ProcessingStatus determineProcessingStatus(
        DeploymentStatus deploymentStatus,
        ReportStatus reportStatus,
        ProcessingStage processingStage
    ) {
        if (deploymentStatus == null || processingStage == null) {
            return null;
        }

        // 계획서 단계
        if (processingStage == ProcessingStage.PLAN) {
            switch (deploymentStatus) {
                case STAGED:
                case PENDING:
                    return ProcessingStatus.PENDING;
                case APPROVED:
                    return ProcessingStatus.APPROVED;
                case REJECTED:
                    return ProcessingStatus.REJECTED;
                case CANCELED:
                    return ProcessingStatus.CANCELED;
                default:
                    return null;
            }
        }

        // 배포 단계
        if (processingStage == ProcessingStage.DEPLOYMENT) {
            switch (deploymentStatus) {
                case DEPLOYMENT:
                    return ProcessingStatus.IN_PROGRESS;
                case COMPLETED:
                    return ProcessingStatus.COMPLETED;
                default:
                    return null;
            }
        }

        // 결과보고 단계
        if (processingStage == ProcessingStage.REPORT) {
            if (reportStatus == null) {
                return ProcessingStatus.PENDING;
            }
            switch (reportStatus) {
                case PENDING:
                    return ProcessingStatus.PENDING;
                case APPROVED:
                    return ProcessingStatus.APPROVED;
                case REJECTED:
                    return ProcessingStatus.REJECTED;
                default:
                    return null;
            }
        }

        return null;
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
