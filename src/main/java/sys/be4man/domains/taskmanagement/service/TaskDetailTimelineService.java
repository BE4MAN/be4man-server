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

/**
 * 작업 상세 - 타임라인 생성 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetailTimelineService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    /**
     * 타임라인 생성 (6단계)
     */
    public List<TimelineStepDto> buildTimeline(Deployment deployment, List<Approval> planApprovals,
                                                List<Approval> reportApprovals, BuildRun buildRun) {
        log.debug("타임라인 생성 - deploymentId: {}", deployment.getId());

        List<TimelineStepDto> timeline = new ArrayList<>();

        // 1. 계획서 작성 (deployment 생성 시각)
        timeline.add(TimelineStepDto.builder()
                .stepNumber(1)
                .stepName("작업 신청")
                .status("completed")
                .timestamp(formatDateTime(deployment.getCreatedAt()))
                .description("완료")
                .build());

        // 2. 계획서 승인 (모든 plan approver가 승인했을 때)
        LocalDateTime planApprovalCompletedAt = getPlanApprovalCompletedTime(planApprovals);
        String planApprovalStatus = planApprovalCompletedAt != null ? "completed" :
                (deployment.getStage() == DeploymentStage.PLAN ? "active" : "pending");
        timeline.add(TimelineStepDto.builder()
                .stepNumber(2)
                .stepName("작업 승인")
                .status(planApprovalStatus)
                .timestamp(formatDateTime(planApprovalCompletedAt))
                .description(planApprovalCompletedAt != null ? "완료" : null)
                .build());

        // 3. 배포 시작
        LocalDateTime deploymentStartedAt = deployment.getScheduledAt();
        boolean deploymentStarted = deploymentStartedAt != null &&
                (deployment.getStage() == DeploymentStage.RETRY ||
                        deployment.getStage() == DeploymentStage.ROLLBACK ||
                        deployment.getStage() == DeploymentStage.REPORT);
        timeline.add(TimelineStepDto.builder()
                .stepNumber(3)
                .stepName("배포시작")
                .status(deploymentStarted ? "completed" :
                        (planApprovalStatus.equals("completed") ? "active" : "pending"))
                .timestamp(formatDateTime(deploymentStartedAt))
                .description(deploymentStarted ? "배포가 시작되었습니다." : null)
                .build());

        // 4. 배포 종료
        LocalDateTime deploymentEndedAt = buildRun != null ? buildRun.getEndedAt() : null;
        boolean deploymentCompleted = deployment.getStatus() == DeploymentStatus.COMPLETED ||
                deployment.getStage() == DeploymentStage.REPORT;
        timeline.add(TimelineStepDto.builder()
                .stepNumber(4)
                .stepName("배포종료")
                .status(deploymentCompleted ? "completed" :
                        (deployment.getStatus() == DeploymentStatus.IN_PROGRESS ? "active" : "pending"))
                .timestamp(formatDateTime(deploymentEndedAt))
                .description(deploymentCompleted ? "배포가 종료되었습니다." : null)
                .build());

        // 5. 결과보고 작성 (stage가 REPORT로 변경된 시점)
        LocalDateTime reportCreatedAt = deployment.getStage() == DeploymentStage.REPORT ?
                deployment.getUpdatedAt() : null;
        timeline.add(TimelineStepDto.builder()
                .stepNumber(5)
                .stepName("결과보고작성")
                .status(reportCreatedAt != null ? "completed" :
                        (deploymentCompleted ? "active" : "pending"))
                .timestamp(formatDateTime(reportCreatedAt))
                .description(reportCreatedAt != null ? "결과보고가 작성되었습니다." : null)
                .build());

        // 6. 결과보고 승인 (모든 report approver가 승인했을 때)
        LocalDateTime reportApprovalCompletedAt = getReportApprovalCompletedTime(reportApprovals);
        timeline.add(TimelineStepDto.builder()
                .stepNumber(6)
                .stepName("결과보고승인")
                .status(reportApprovalCompletedAt != null ? "completed" :
                        (deployment.getStage() == DeploymentStage.REPORT ? "active" : "pending"))
                .timestamp(formatDateTime(reportApprovalCompletedAt))
                .description(reportApprovalCompletedAt != null ? "결과보고 승인이 완료되었습니다." : null)
                .build());

        log.debug("타임라인 생성 완료 - {} 단계", timeline.size());
        return timeline;
    }

    /**
     * 계획서 승인 완료 시각 조회
     */
    private LocalDateTime getPlanApprovalCompletedTime(List<Approval> planApprovals) {
        if (planApprovals.isEmpty()) {
            return null;
        }

        // 첫 번째 Approval의 ApprovalLine 확인
        Approval approval = planApprovals.get(0);

        // 모든 승인자가 승인했는지 확인
        boolean allApproved = approval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)  // 참조 제외
                .allMatch(line -> {
                    Boolean isApproved = line.getIsApproved();
                    return isApproved != null && isApproved;
                });

        if (!allApproved) {
            return null;
        }

        // 마지막 승인자의 처리 시각 반환 (approval.approvedAt 사용)
        return approval.getApprovedAt();
    }

    /**
     * 결과보고 승인 완료 시각 조회
     */
    private LocalDateTime getReportApprovalCompletedTime(List<Approval> reportApprovals) {
        if (reportApprovals.isEmpty()) {
            return null;
        }

        Approval approval = reportApprovals.get(0);

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
     * LocalDateTime을 yyyy.MM.dd HH:mm 형식으로 포맷
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }
}