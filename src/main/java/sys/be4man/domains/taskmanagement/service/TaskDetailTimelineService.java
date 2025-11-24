package sys.be4man.domains.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.approval.model.type.ApprovalLineType;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.taskmanagement.dto.TimelineStepDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetailTimelineService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    // TaskDetailTimelineService.java - buildTimeline() 메서드 수정

    public List<TimelineStepDto> buildTimeline(Deployment deployment, List<Approval> planApprovals,
            List<Approval> reportApprovals, BuildRun buildRun) {
        log.debug("타임라인 생성 - deploymentId: {}", deployment.getId());

        List<TimelineStepDto> timeline = new ArrayList<>();

        // ✅ 배포 타입 텍스트 결정
        String deploymentTypeText = "";
        if (deployment.getStage() == DeploymentStage.ROLLBACK) {
            deploymentTypeText = " (롤백)";
        } else if (deployment.getStage() == DeploymentStage.RETRY) {
            deploymentTypeText = " (재배포)";
        }

        // 1. 작업 신청
        timeline.add(TimelineStepDto.builder()
                .stepNumber(1)
                .stepName("작업 신청")
                .status("completed")
                .result(null)
                .timestamp(formatDateTime(deployment.getCreatedAt()))
                .description("완료")
                .build());

        // 2. 작업 승인
        String planApprovalStatus = "pending";
        LocalDateTime planApprovalCompletedAt = null;
        String planApprovalDescription = null;
        String planApprovalResult = null;

        // ✅ 취소된 작업은 먼저 확인
        if (deployment.getStatus() == DeploymentStatus.CANCELED) {
            planApprovalStatus = "completed";
            planApprovalResult = "failure";
            planApprovalCompletedAt = deployment.getUpdatedAt();
            planApprovalDescription = "취소";
            log.debug("타임라인 - 작업 취소: deploymentId={}", deployment.getId());
        } else if (!planApprovals.isEmpty()) {
            Approval approval = planApprovals.get(0);

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

            boolean allApproved = approval.getApprovalLines().stream()
                    .filter(line -> line.getType() != ApprovalLineType.CC)
                    .allMatch(line -> {
                        Boolean isApproved = line.getIsApproved();
                        return isApproved != null && isApproved;
                    });

            if (isRejectedByApprovalLine) {
                planApprovalStatus = "completed";
                planApprovalResult = "failure";
                planApprovalCompletedAt = rejectedAt != null ? rejectedAt : deployment.getUpdatedAt();
                planApprovalDescription = "반려";
            } else if (allApproved) {
                planApprovalStatus = "completed";
                planApprovalResult = "success";
                planApprovalCompletedAt = approval.getApprovedAt() != null ? approval.getApprovedAt() : deployment.getUpdatedAt();
                planApprovalDescription = "승인 완료";
            } else {
                planApprovalStatus = "active";
                planApprovalDescription = "승인 대기중";
            }
        } else {
            if (deployment.getStatus() == DeploymentStatus.APPROVED) {
                planApprovalStatus = "completed";
                planApprovalResult = "success";
                planApprovalCompletedAt = deployment.getUpdatedAt();
                planApprovalDescription = "승인 완료";
            } else if (deployment.getStatus() == DeploymentStatus.REJECTED) {
                planApprovalStatus = "completed";
                planApprovalResult = "failure";
                planApprovalCompletedAt = deployment.getUpdatedAt();
                planApprovalDescription = "반려";
            } else if (deployment.getStatus() == DeploymentStatus.PENDING &&
                    deployment.getStage() == DeploymentStage.PLAN) {
                planApprovalStatus = "active";
                planApprovalDescription = "승인 대기중";
            } else if (deployment.getStage() == DeploymentStage.DEPLOYMENT ||
                    deployment.getStage() == DeploymentStage.REPORT ||
                    deployment.getStage() == DeploymentStage.RETRY ||
                    deployment.getStage() == DeploymentStage.ROLLBACK) {
                planApprovalStatus = "completed";
                planApprovalResult = "success";
                planApprovalCompletedAt = deployment.getUpdatedAt();
                planApprovalDescription = "승인 완료";
            }
        }

        timeline.add(TimelineStepDto.builder()
                .stepNumber(2)
                .stepName("작업 승인")
                .status(planApprovalStatus)
                .result(planApprovalResult)
                .timestamp(formatDateTime(planApprovalCompletedAt))
                .description(planApprovalDescription)
                .build());

        // 3. 배포중 ✅ (재배포)/(롤백) 추가
        LocalDateTime deploymentStartedAt = null;
        String deploymentStatus = "pending";
        String deploymentStartDescription = null;

        boolean isDeploymentStarted = (deployment.getStage() == DeploymentStage.DEPLOYMENT ||
                deployment.getStage() == DeploymentStage.RETRY ||
                deployment.getStage() == DeploymentStage.ROLLBACK ||
                deployment.getStage() == DeploymentStage.REPORT);

        if (isDeploymentStarted) {
            deploymentStartedAt = deployment.getScheduledAt();

            if (deployment.getStatus() == DeploymentStatus.IN_PROGRESS) {
                deploymentStatus = "active";
                deploymentStartDescription = "배포 진행중";
            } else if (deployment.getStatus() == DeploymentStatus.COMPLETED ||
                    deployment.getStage() == DeploymentStage.REPORT) {
                deploymentStatus = "completed";
                deploymentStartDescription = "배포 완료";
            }
        } else if (planApprovalStatus.equals("completed") &&
                planApprovalResult != null &&
                planApprovalResult.equals("success")) {
            deploymentStatus = "pending";
        }

        timeline.add(TimelineStepDto.builder()
                .stepNumber(3)
                .stepName("배포중" + deploymentTypeText)  // ✅ 배포 타입 추가
                .status(deploymentStatus)
                .result(null)
                .timestamp(deploymentStartedAt != null ? formatDateTime(deploymentStartedAt) : null)
                .description(deploymentStartDescription)
                .build());

        // 4. 배포 종료 ✅ (재배포)/(롤백) 추가
        LocalDateTime deploymentEndedAt = null;
        String deploymentEndStatus = "pending";
        String deploymentEndResult = null;
        String deploymentEndDescription = null;

        if (deployment.getStatus() == DeploymentStatus.COMPLETED) {
            deploymentEndStatus = "completed";

            if (buildRun != null && buildRun.getEndedAt() != null) {
                deploymentEndedAt = buildRun.getEndedAt();
            } else {
                deploymentEndedAt = deployment.getUpdatedAt();
            }

            if (deployment.getIsDeployed() != null) {
                if (deployment.getIsDeployed()) {
                    deploymentEndResult = "success";
                    deploymentEndDescription = "배포 성공";
                } else {
                    deploymentEndResult = "failure";
                    deploymentEndDescription = "배포 실패";
                }
            } else {
                deploymentEndResult = "success";
                deploymentEndDescription = "배포 완료";
            }
        } else if (deployment.getStage() == DeploymentStage.REPORT) {
            deploymentEndStatus = "completed";

            if (buildRun != null && buildRun.getEndedAt() != null) {
                deploymentEndedAt = buildRun.getEndedAt();
            } else {
                deploymentEndedAt = deployment.getUpdatedAt();
            }

            if (deployment.getIsDeployed() != null) {
                deploymentEndResult = deployment.getIsDeployed() ? "success" : "failure";
                deploymentEndDescription = deployment.getIsDeployed() ? "배포 성공" : "배포 실패";
            } else {
                deploymentEndResult = "success";
                deploymentEndDescription = "배포 완료";
            }
        }

        timeline.add(TimelineStepDto.builder()
                .stepNumber(4)
                .stepName("배포종료" + deploymentTypeText)  // ✅ 배포 타입 추가
                .status(deploymentEndStatus)
                .result(deploymentEndResult)
                .timestamp(formatDateTime(deploymentEndedAt))
                .description(deploymentEndDescription)
                .build());

        // 5. 결과보고 작성
        LocalDateTime reportCreatedAt = null;
        String reportCreatedStatus = "pending";
        String reportCreatedDescription = null;

        if (deployment.getStage() == DeploymentStage.REPORT) {
            reportCreatedAt = deployment.getUpdatedAt();
            reportCreatedStatus = "completed";
            reportCreatedDescription = "결과보고 작성 완료";
        } else if (deploymentEndStatus.equals("completed")) {
            reportCreatedStatus = "pending";
        }

        timeline.add(TimelineStepDto.builder()
                .stepNumber(5)
                .stepName("결과보고작성")
                .status(reportCreatedStatus)
                .result(null)
                .timestamp(formatDateTime(reportCreatedAt))
                .description(reportCreatedDescription)
                .build());

        // 6. 결과보고 승인
        String reportApprovalStatus = "pending";
        String reportApprovalDescription = null;
        String reportApprovalResult = null;
        LocalDateTime reportApprovalCompletedAt = null;

        boolean isReportRejected = false;
        boolean isReportAllApproved = false;
        LocalDateTime reportRejectedAt = null;

        if (!reportApprovals.isEmpty() && deployment.getStage() == DeploymentStage.REPORT) {
            Approval reportApproval = reportApprovals.get(0);

            for (ApprovalLine line : reportApproval.getApprovalLines()) {
                if (line.getType() != ApprovalLineType.CC) {
                    Boolean isApproved = line.getIsApproved();
                    if (isApproved != null && !isApproved) {
                        isReportRejected = true;
                        reportRejectedAt = line.getApprovedAt();
                        break;
                    }
                }
            }

            isReportAllApproved = reportApproval.getApprovalLines().stream()
                    .filter(line -> line.getType() != ApprovalLineType.CC)
                    .allMatch(line -> {
                        Boolean isApproved = line.getIsApproved();
                        return isApproved != null && isApproved;
                    });

            if (isReportRejected) {
                reportApprovalStatus = "completed";
                reportApprovalResult = "failure";
                reportApprovalCompletedAt = reportRejectedAt != null ? reportRejectedAt : deployment.getUpdatedAt();
                reportApprovalDescription = "반려";
            } else if (isReportAllApproved) {
                reportApprovalStatus = "completed";
                reportApprovalResult = "success";
                reportApprovalCompletedAt = reportApproval.getApprovedAt() != null ?
                        reportApproval.getApprovedAt() : deployment.getUpdatedAt();
                reportApprovalDescription = "승인 완료";
            } else {
                reportApprovalStatus = "active";
                reportApprovalDescription = "승인 대기중";
            }
        } else if (deployment.getStage() == DeploymentStage.REPORT) {
            if (deployment.getStatus() == DeploymentStatus.COMPLETED) {
                reportApprovalStatus = "completed";
                reportApprovalResult = "success";
                reportApprovalCompletedAt = deployment.getUpdatedAt();
                reportApprovalDescription = "승인 완료";
            } else if (deployment.getStatus() == DeploymentStatus.PENDING) {
                reportApprovalStatus = "active";
                reportApprovalDescription = "승인 대기중";
            } else if (deployment.getStatus() == DeploymentStatus.REJECTED) {
                reportApprovalStatus = "completed";
                reportApprovalResult = "failure";
                reportApprovalCompletedAt = deployment.getUpdatedAt();
                reportApprovalDescription = "반려";
            }
        } else if (reportCreatedStatus.equals("completed")) {
            reportApprovalStatus = "pending";
        }

        timeline.add(TimelineStepDto.builder()
                .stepNumber(6)
                .stepName("결과보고승인")
                .status(reportApprovalStatus)
                .result(reportApprovalResult)
                .timestamp(formatDateTime(reportApprovalCompletedAt))
                .description(reportApprovalDescription)
                .build());

        log.debug("타임라인 생성 완료 - {} 단계", timeline.size());
        return timeline;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }
}