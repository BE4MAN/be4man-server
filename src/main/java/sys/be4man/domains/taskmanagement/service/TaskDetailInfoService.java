package sys.be4man.domains.taskmanagement.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.approval.model.type.ApprovalLineType;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.taskmanagement.dto.PlanContentDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetailInfoService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public PlanContentDto buildPlanContent(Deployment deployment, List<Approval> planApprovals, BuildRun buildRun) {
        log.debug("계획서 내용 생성 - deploymentId: {}, planApprovals: {}", deployment.getId(), planApprovals.size());

        String content = deployment.getContent();
        String planStatus = "승인대기";

        // ✅ ApprovalLine 우선 확인
        if (!planApprovals.isEmpty()) {
            Approval planApproval = planApprovals.get(0);
            content = planApproval.getContent();

            log.debug("Approval 정보 - id: {}, lines: {}", planApproval.getId(), planApproval.getApprovalLines().size());

            // 반려 확인
            boolean isRejected = false;
            for (ApprovalLine line : planApproval.getApprovalLines()) {
                if (line.getType() != ApprovalLineType.CC) {
                    Boolean isApproved = line.getIsApproved();
                    log.debug("  ApprovalLine - account: {}, isApproved: {}",
                            line.getAccount().getName(), isApproved);

                    if (isApproved != null && !isApproved) {
                        isRejected = true;
                        break;
                    }
                }
            }

            // 전체 승인 완료 확인
            boolean allApproved = planApproval.getApprovalLines().stream()
                    .filter(line -> line.getType() != ApprovalLineType.CC)
                    .allMatch(line -> {
                        Boolean isApproved = line.getIsApproved();
                        return isApproved != null && isApproved;
                    });

            log.debug("isRejected: {}, allApproved: {}", isRejected, allApproved);

            if (isRejected) {
                planStatus = "반려";
            } else if (allApproved) {
                planStatus = "승인완료";
            } else {
                planStatus = "승인대기";
            }
        } else {
            // ✅ Approval이 없으면 Deployment 상태 확인
            log.debug("planApprovals 없음 - Deployment.status로 판단: {}", deployment.getStatus());

            if (deployment.getStatus() == DeploymentStatus.APPROVED) {
                planStatus = "승인완료";
            } else if (deployment.getStatus() == DeploymentStatus.REJECTED) {
                planStatus = "반려";
            } else if (deployment.getStatus() == DeploymentStatus.PENDING) {
                planStatus = "승인대기";
            }
        }

        log.debug("최종 planStatus: {}", planStatus);

        // ✅ 배포 종료 시각 결정 로직 개선
        LocalDateTime actualEndedAt = null;

        // 1. BuildRun의 종료 시각 우선 사용 (가장 정확한 실제 종료 시각)
        if (buildRun != null && buildRun.getEndedAt() != null) {
            actualEndedAt = buildRun.getEndedAt();
            log.debug("배포 종료 시각: BuildRun.endedAt 사용 - {}", actualEndedAt);
        }
        // 2. Deployment가 COMPLETED 상태면 updatedAt 사용 (실제 완료 처리 시각)
        else if (deployment.getStatus() == DeploymentStatus.COMPLETED && deployment.getUpdatedAt() != null) {
            actualEndedAt = deployment.getUpdatedAt();
            log.debug("배포 종료 시각: Deployment.updatedAt 사용 (COMPLETED 상태) - {}", actualEndedAt);
        }
        // 3. 계획된 종료 시각 사용
        else {
            actualEndedAt = deployment.getScheduledToEndedAt();
            log.debug("배포 종료 시각: scheduledToEndedAt 사용 (계획 시각) - {}", actualEndedAt);
        }

        return PlanContentDto.builder()
                .drafter(deployment.getIssuer() != null ? deployment.getIssuer().getName() : null)
                .department(deployment.getIssuer() != null && deployment.getIssuer().getDepartment() != null ?
                        deployment.getIssuer().getDepartment().getKoreanName() : null)
                .createdAt(formatDateTime(deployment.getCreatedAt()))
                .serviceName(deployment.getProject() != null ? deployment.getProject().getName() : null)
                .taskTitle(deployment.getTitle())
                .scheduledAt(formatDateTime(deployment.getScheduledAt()))
                .scheduledToEndedAt(formatDateTime(actualEndedAt))
                .expectedDuration(deployment.getExpectedDuration())
                .version(deployment.getVersion())
                .content(content)
                .planStatus(planStatus)
                .build();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }
}