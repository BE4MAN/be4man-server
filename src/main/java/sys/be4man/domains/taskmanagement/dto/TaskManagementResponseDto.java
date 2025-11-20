package sys.be4man.domains.taskmanagement.dto;

import lombok.*;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.approval.model.type.ApprovalLineType;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.model.type.DeploymentStage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskManagementResponseDto {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private Long id;
    private String drafter;
    private String department;
    private String serviceName;
    private String taskTitle;
    private String stage;
    private String status;
    private String completionTime;
    private String result;

    public TaskManagementResponseDto(Deployment deployment) {
        this(deployment, null, null);
    }

    public TaskManagementResponseDto(Deployment deployment, List<Approval> approvals) {
        this(deployment, approvals, null);
    }

    // ✅ reportApprovals 파라미터 추가
    public TaskManagementResponseDto(Deployment deployment, List<Approval> planApprovals, List<Approval> reportApprovals) {
        this.id = deployment.getId();
        this.drafter = deployment.getIssuer() != null ? deployment.getIssuer().getName() : null;
        this.department = deployment.getIssuer() != null && deployment.getIssuer().getDepartment() != null
                ? deployment.getIssuer().getDepartment().getKoreanName() : null;
        this.serviceName = deployment.getProject() != null ? deployment.getProject().getName() : null;
        this.taskTitle = deployment.getTitle();
        this.stage = getDeploymentStageKorean(deployment.getStage());

        // ✅ planApprovals와 reportApprovals 모두 전달
        determineStatusAndTime(deployment, planApprovals, reportApprovals);

        this.result = getDeploymentResult(deployment);
    }

    /**
     * ✅ ApprovalLine을 확인하여 실제 상태 결정 (PLAN, REPORT 모두 확인)
     */
    private void determineStatusAndTime(Deployment deployment, List<Approval> planApprovals, List<Approval> reportApprovals) {
        DeploymentStage stage = deployment.getStage();

        // ✅ 결과보고 단계일 때는 reportApprovals 확인
        if (stage == DeploymentStage.REPORT && reportApprovals != null && !reportApprovals.isEmpty()) {
            Approval reportApproval = reportApprovals.get(0);

            // 반려 확인
            boolean isRejected = false;
            LocalDateTime rejectedAt = null;
            for (ApprovalLine line : reportApproval.getApprovalLines()) {
                if (line.getType() != ApprovalLineType.CC) {
                    Boolean isApproved = line.getIsApproved();
                    if (isApproved != null && !isApproved) {
                        isRejected = true;
                        rejectedAt = line.getApprovedAt();
                        break;
                    }
                }
            }

            if (isRejected) {
                this.status = "반려";
                this.completionTime = formatDateTime(rejectedAt != null ? rejectedAt : deployment.getUpdatedAt());
                return;
            }

            // 전체 승인 완료 확인
            boolean allApproved = reportApproval.getApprovalLines().stream()
                    .filter(line -> line.getType() != ApprovalLineType.CC)
                    .allMatch(line -> {
                        Boolean isApproved = line.getIsApproved();
                        return isApproved != null && isApproved;
                    });

            if (allApproved) {
                this.status = "승인";
                this.completionTime = formatDateTime(reportApproval.getApprovedAt() != null ?
                        reportApproval.getApprovedAt() : deployment.getUpdatedAt());
                return;
            }

            // 대기중
            this.status = "대기";
            this.completionTime = null;
            return;
        }

        // ✅ 계획서 단계일 때는 planApprovals 확인
        if (planApprovals != null && !planApprovals.isEmpty()) {
            Approval approval = planApprovals.get(0);

            // 반려 확인
            boolean isRejectedByApprovalLine = false;
            LocalDateTime rejectedAt = null;
            for (ApprovalLine line : approval.getApprovalLines()) {
                if (line.getType() != ApprovalLineType.CC) {
                    Boolean isApproved = line.getIsApproved();
                    if (isApproved != null && !isApproved) {
                        isRejectedByApprovalLine = true;
                        rejectedAt = line.getApprovedAt();
                        break;
                    }
                }
            }

            if (isRejectedByApprovalLine) {
                this.status = "반려";
                this.completionTime = formatDateTime(rejectedAt != null ? rejectedAt : deployment.getUpdatedAt());
                return;
            }

            // 전체 승인 완료 확인
            boolean allApproved = approval.getApprovalLines().stream()
                    .filter(line -> line.getType() != ApprovalLineType.CC)
                    .allMatch(line -> {
                        Boolean isApproved = line.getIsApproved();
                        return isApproved != null && isApproved;
                    });

            if (allApproved && stage == DeploymentStage.PLAN) {
                this.status = "승인";
                this.completionTime = formatDateTime(approval.getApprovedAt() != null ?
                        approval.getApprovedAt() : deployment.getUpdatedAt());
                return;
            }
        }

        // ✅ 기본 로직 (Deployment 상태 기반)
        DeploymentStatus deploymentStatus = deployment.getStatus();

        switch (deploymentStatus) {
            case PENDING:
                this.status = "대기";
                this.completionTime = null;
                break;
            case APPROVED:
                this.status = "승인";
                this.completionTime = formatDateTime(deployment.getUpdatedAt());
                break;
            case IN_PROGRESS:
                this.status = "진행중";
                this.completionTime = null;
                break;
            case COMPLETED:
                this.status = "완료";
                this.completionTime = formatDateTime(deployment.getUpdatedAt());
                break;
            case REJECTED:
                this.status = "반려";
                this.completionTime = formatDateTime(deployment.getUpdatedAt());
                break;
            case CANCELED:
                this.status = "취소";
                this.completionTime = formatDateTime(deployment.getUpdatedAt());
                break;
            default:
                this.status = deploymentStatus.getKoreanName();
                this.completionTime = null;
        }
    }

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

    private String getDeploymentResult(Deployment deployment) {
        if (deployment.getStatus() == DeploymentStatus.COMPLETED && deployment.getIsDeployed() != null) {
            return deployment.getIsDeployed() ? "성공" : "실패";
        }
        return null;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }
}