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

    /**
     * ✅ 수정: Deployment 기반 타임라인 생성
     * - planApprovals, reportApprovals는 상세 정보 참조용으로만 사용
     */
    public List<TimelineStepDto> buildTimeline(
            Deployment deployment,
            List<Approval> planApprovals,
            List<Approval> reportApprovals,
            BuildRun buildRun
    ) {
        log.debug("타임라인 생성 - deploymentId: {}", deployment.getId());

        List<TimelineStepDto> timeline = new ArrayList<>();

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

// ✅ 승인 라인에서 거절 여부 먼저 체크
        boolean isPlanRejected = checkIfRejected(planApprovals);

        if (isPlanRejected) {
            // ✅ 승인 라인에서 거절된 경우
            planApprovalStatus = "completed";
            planApprovalResult = "failure";
            planApprovalCompletedAt = getRejectedTime(planApprovals);
            if (planApprovalCompletedAt == null) {
                planApprovalCompletedAt = deployment.getUpdatedAt();
            }
            planApprovalDescription = "반려";
        } else if (deployment.getStatus() == DeploymentStatus.APPROVED) {
            planApprovalStatus = "completed";
            planApprovalResult = "success";
            planApprovalCompletedAt = getPlanApprovalCompletedTime(planApprovals);
            if (planApprovalCompletedAt == null) {
                planApprovalCompletedAt = deployment.getUpdatedAt();
            }
            planApprovalDescription = "승인 완료";
        } else if (deployment.getStatus() == DeploymentStatus.PENDING &&
                deployment.getStage() == DeploymentStage.PLAN) {
            planApprovalStatus = "active";
            planApprovalResult = null;
            planApprovalCompletedAt = null;
            planApprovalDescription = "승인 대기중";
        } else if (deployment.getStatus() == DeploymentStatus.REJECTED) {
            planApprovalStatus = "completed";
            planApprovalResult = "failure";
            planApprovalCompletedAt = deployment.getUpdatedAt();
            planApprovalDescription = "반려";
        } else if (deployment.getStage() == DeploymentStage.DEPLOYMENT ||
                deployment.getStage() == DeploymentStage.REPORT ||
                deployment.getStage() == DeploymentStage.RETRY ||
                deployment.getStage() == DeploymentStage.ROLLBACK) {
            planApprovalStatus = "completed";
            planApprovalResult = "success";
            planApprovalCompletedAt = getPlanApprovalCompletedTime(planApprovals);
            if (planApprovalCompletedAt == null) {
                planApprovalCompletedAt = deployment.getUpdatedAt();
            }
            planApprovalDescription = "승인 완료";
        }

        timeline.add(TimelineStepDto.builder()
                .stepNumber(2)
                .stepName("작업 승인")
                .status(planApprovalStatus)
                .result(planApprovalResult)
                .timestamp(formatDateTime(planApprovalCompletedAt))
                .description(planApprovalDescription)
                .build());


        // 3. 배포중
        LocalDateTime deploymentStartedAt = deployment.getScheduledAt();
        String deploymentStatus = "pending";
        String deploymentStartDescription = null;

        boolean deploymentStarted = deploymentStartedAt != null &&
                (deployment.getStage() == DeploymentStage.DEPLOYMENT ||
                        deployment.getStage() == DeploymentStage.RETRY ||
                        deployment.getStage() == DeploymentStage.ROLLBACK ||
                        deployment.getStage() == DeploymentStage.REPORT);

        if (deploymentStarted) {
            if (deployment.getStatus() == DeploymentStatus.IN_PROGRESS) {
                deploymentStatus = "active";
                deploymentStartDescription = "배포 진행중";
            } else if (deployment.getStatus() == DeploymentStatus.COMPLETED ||
                    deployment.getStage() == DeploymentStage.REPORT) {
                deploymentStatus = "completed";
                deploymentStartDescription = "배포 시작";
            }
        } else if (planApprovalStatus.equals("completed")) {
            deploymentStatus = "pending";
            deploymentStartDescription = null;
        }

        timeline.add(TimelineStepDto.builder()
                .stepNumber(3)
                .stepName("배포중")
                .status(deploymentStatus)
                .result(null)
                .timestamp(deploymentStarted ? formatDateTime(deploymentStartedAt) : null)
                .description(deploymentStartDescription)
                .build());

        // 4. 배포 종료
        LocalDateTime deploymentEndedAt = buildRun != null ? buildRun.getEndedAt()
                : deployment.getScheduledToEndedAt();

        String deploymentEndStatus = "pending";
        String deploymentEndResult = null;
        String deploymentEndDescription = null;

// ✅ 배포 상태에 따라 종료 결과 표시
        if (deployment.getStatus() == DeploymentStatus.COMPLETED) {
            deploymentEndStatus = "completed";

            // ✅ endedAt이 없으면 현재 시간 사용
            if (deploymentEndedAt == null) {
                deploymentEndedAt = deployment.getUpdatedAt();
            }

            // ✅ 배포 성공/실패 판정
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
            // 결과보고 단계면 배포는 완료된 것
            deploymentEndStatus = "completed";

            if (deploymentEndedAt == null) {
                deploymentEndedAt = deployment.getUpdatedAt();
            }

            if (deployment.getIsDeployed() != null) {
                deploymentEndResult = deployment.getIsDeployed() ? "success" : "failure";
                deploymentEndDescription = deployment.getIsDeployed() ? "배포 성공" : "배포 실패";
            } else {
                deploymentEndResult = "success";
                deploymentEndDescription = "배포 완료";
            }
        } else if (deployment.getStatus() == DeploymentStatus.IN_PROGRESS) {
            // IN_PROGRESS면 배포중 상태 유지
            deploymentEndStatus = "active";
            deploymentEndDescription = "배포 진행중";
        }

        timeline.add(TimelineStepDto.builder()
                .stepNumber(4)
                .stepName("배포종료")
                .status(deploymentEndStatus)
                .result(deploymentEndResult)
                .timestamp(formatDateTime(deploymentEndedAt))  // ✅ 이제 시간 표시됨
                .description(deploymentEndDescription)
                .build());

        // 5. 결과보고 작성
        LocalDateTime reportCreatedAt = null;
        String reportCreatedStatus = "pending";
        String reportCreatedDescription = null;

        if (deployment.getStage() == DeploymentStage.REPORT) {
            // 결과보고가 있으면 작성 완료
            if (!reportApprovals.isEmpty()) {
                Approval reportApproval = reportApprovals.get(0);
                reportCreatedAt = reportApproval.getCreatedAt();
            } else {
                reportCreatedAt = deployment.getUpdatedAt();
            }
            reportCreatedStatus = "completed";
            reportCreatedDescription = "결과보고 작성 완료";
        } else if (deploymentEndStatus.equals("completed")) {
            // 배포가 완료되었으면 대기중
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
        LocalDateTime reportApprovalCompletedAt = null;

        if (deployment.getStage() == DeploymentStage.REPORT) {
            if (deployment.getStatus() == DeploymentStatus.COMPLETED) {
                // 승인 완료
                reportApprovalStatus = "completed";
                reportApprovalCompletedAt = getReportApprovalCompletedTime(reportApprovals);
                if (reportApprovalCompletedAt == null) {
                    reportApprovalCompletedAt = deployment.getUpdatedAt();
                }
                reportApprovalDescription = "승인 완료";
            } else if (deployment.getStatus() == DeploymentStatus.PENDING) {
                // 승인 대기중
                reportApprovalStatus = "active";
                reportApprovalDescription = "승인 대기중";
            } else if (deployment.getStatus() == DeploymentStatus.REJECTED) {
                // 반려
                reportApprovalStatus = "rejected";
                reportApprovalCompletedAt = deployment.getUpdatedAt();
                reportApprovalDescription = "반려";
            }
        } else if (reportCreatedStatus.equals("completed")) {
            // 결과보고가 작성되었으면 대기중
            reportApprovalStatus = "pending";
        }

        timeline.add(TimelineStepDto.builder()
                .stepNumber(6)
                .stepName("결과보고승인")
                .status(reportApprovalStatus)
                .result(null)
                .timestamp(formatDateTime(reportApprovalCompletedAt))
                .description(reportApprovalDescription)
                .build());

        log.debug("타임라인 생성 완료 - {} 단계", timeline.size());
        return timeline;
    }

    /**
     * 계획서 승인 완료 시각 가져오기 (Approval에서)
     */
    private LocalDateTime getPlanApprovalCompletedTime(List<Approval> planApprovals) {
        if (planApprovals.isEmpty()) {
            return null;
        }

        Approval approval = planApprovals.get(0);

        // 모든 승인자가 승인했는지 확인
        boolean allApproved = approval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)
                .allMatch(line -> {
                    Boolean isApproved = line.getIsApproved();
                    return isApproved != null && isApproved;
                });

        if (!allApproved) {
            return null;
        }

        return approval.getApprovedAt();
    }

    /**
     * 결과보고 승인 완료 시각 가져오기 (Approval에서)
     */
    private LocalDateTime getReportApprovalCompletedTime(List<Approval> reportApprovals) {
        if (reportApprovals.isEmpty()) {
            return null;
        }

        Approval approval = reportApprovals.get(0);

        // 모든 승인자가 승인했는지 확인
        boolean allApproved = approval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)
                .allMatch(line -> {
                    Boolean isApproved = line.getIsApproved();
                    return isApproved != null && isApproved;
                });

        if (!allApproved) {
            return null;
        }

        return approval.getApprovedAt();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }


    /**
     * 승인 라인에서 거절 여부 확인
     */
    private boolean checkIfRejected(List<Approval> approvals) {
        if (approvals.isEmpty()) {
            return false;
        }

        Approval approval = approvals.get(0);
        return approval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)
                .anyMatch(line -> {
                    Boolean isApproved = line.getIsApproved();
                    return isApproved != null && !isApproved;
                });
    }

    /**
     * 거절된 시간 가져오기
     */
    private LocalDateTime getRejectedTime(List<Approval> approvals) {
        if (approvals.isEmpty()) {
            return null;
        }

        Approval approval = approvals.get(0);
        return approval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)
                .filter(line -> {
                    Boolean isApproved = line.getIsApproved();
                    return isApproved != null && !isApproved;
                })
                .map(ApprovalLine::getUpdatedAt)
                .findFirst()
                .orElse(null);
    }
}