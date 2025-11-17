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
import sys.be4man.domains.taskmanagement.dto.PlanContentDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetailInfoService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public PlanContentDto buildPlanContent(Deployment deployment, List<Approval> planApprovals, BuildRun buildRun) {
        String content = deployment.getContent();
        String planStatus = "대기"; // ✅ 기본값

        if (!planApprovals.isEmpty()) {
            Approval planApproval = planApprovals.get(0);
            content = planApproval.getContent();

            // ✅ 반려 여부 확인
            boolean isRejected = planApproval.getApprovalLines().stream()
                    .filter(line -> line.getType() != ApprovalLineType.CC)
                    .anyMatch(line -> {
                        Boolean isApproved = line.getIsApproved();
                        return isApproved != null && !isApproved;
                    });

            // ✅ 전체 승인 완료 여부 확인
            boolean allApproved = planApproval.getApprovalLines().stream()
                    .filter(line -> line.getType() != ApprovalLineType.CC)
                    .allMatch(line -> {
                        Boolean isApproved = line.getIsApproved();
                        return isApproved != null && isApproved;
                    });

            if (isRejected) {
                planStatus = "반려";
            } else if (allApproved) {
                planStatus = "승인";
            } else {
                planStatus = "대기";
            }
        }

        LocalDateTime actualEndedAt = null;
        if (buildRun != null && buildRun.getEndedAt() != null) {
            actualEndedAt = buildRun.getEndedAt();
        }

        return PlanContentDto.builder()
                .drafter(deployment.getIssuer() != null ? deployment.getIssuer().getName() : null)
                .department(deployment.getIssuer() != null && deployment.getIssuer().getDepartment() != null ?
                        deployment.getIssuer().getDepartment().getKoreanName() : null)
                .createdAt(formatDateTime(deployment.getCreatedAt()))
                .serviceName(deployment.getProject() != null ? deployment.getProject().getName() : null)
                .taskTitle(deployment.getTitle())
                .scheduledAt(formatDateTime(deployment.getScheduledAt()))
                .scheduledToEndedAt(formatDateTime(actualEndedAt != null ? actualEndedAt : deployment.getScheduledToEndedAt()))
                .riskDescription(null)
                .expectedDuration(deployment.getExpectedDuration())
                .version(deployment.getVersion())
                .strategy(null)
                .content(content)
                .planStatus(planStatus) // ✅ 계획서 상태 추가
                .build();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    private String formatDuration(Long durationMs) {
        if (durationMs == null) {
            return null;
        }

        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%d분 %d초", minutes, remainingSeconds);
        } else {
            return String.format("%d초", remainingSeconds);
        }
    }
}