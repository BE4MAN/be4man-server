package sys.be4man.domains.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.taskmanagement.dto.ReportContentDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetailReportService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public ReportContentDto buildReportContent(Deployment deployment, BuildRun buildRun, List<Approval> reportApprovals) {
        log.debug("=== 결과보고 내용 생성 시작 ===");
        log.debug("deploymentId: {}", deployment.getId());
        log.debug("deployment.stage: {}", deployment.getStage());
        log.debug("reportApprovals.size(): {}", reportApprovals.size());

        // ✅ REPORT 단계가 아니면 null 반환
        if (deployment.getStage() != DeploymentStage.REPORT) {
            log.debug("REPORT 단계가 아님 - null 반환");
            return null;
        }

        String deploymentResult = null;
        if (deployment.getIsDeployed() != null) {
            deploymentResult = deployment.getIsDeployed() ? "성공" : "실패";
        }
        log.debug("deploymentResult: {}", deploymentResult);

        // ✅ 결과보고 내용
        String reportContent = deployment.getContent();
        LocalDateTime reportCreatedAt = deployment.getCreatedAt();

        log.debug("초기 reportContent (Deployment에서): {}", reportContent);
        log.debug("초기 reportContent type: {}", reportContent != null ? reportContent.getClass().getName() : "null");
        log.debug("초기 reportContent length: {}", reportContent != null ? reportContent.length() : 0);

        if (!reportApprovals.isEmpty()) {
            Approval reportApproval = reportApprovals.get(0);
            reportContent = reportApproval.getContent();
            reportCreatedAt = reportApproval.getCreatedAt();
            log.debug("✅ 결과보고 Approval 사용");
            log.debug("approvalId: {}", reportApproval.getId());
            log.debug("approval.content: {}", reportContent);
            log.debug("approval.content type: {}", reportContent != null ? reportContent.getClass().getName() : "null");
        } else {
            log.debug("⚠️ 결과보고 Approval 없음 - Deployment.content 사용");
        }

        log.debug("최종 reportContent: {}", reportContent);
        log.debug("최종 reportContent 미리보기: {}",
                reportContent != null && reportContent.length() > 100
                        ? reportContent.substring(0, 100) + "..."
                        : reportContent);

        ReportContentDto dto = ReportContentDto.builder()
                .deploymentResult(deploymentResult)
                .actualStartedAt(buildRun != null ? formatDateTime(buildRun.getStartedAt()) : null)
                .actualEndedAt(buildRun != null ? formatDateTime(buildRun.getEndedAt()) : null)
                .actualDuration(buildRun != null ? formatDuration(buildRun.getDuration()) : null)
                .reportContent(reportContent)
                .reportCreatedAt(formatDateTime(reportCreatedAt))
                .build();

        log.debug("=== ReportContentDto 생성 완료 ===");
        log.debug("DTO: {}", dto);

        return dto;
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