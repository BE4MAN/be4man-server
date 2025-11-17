package sys.be4man.domains.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.taskmanagement.dto.ReportContentDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 작업 상세 - 결과보고 생성 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetailReportService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    /**
     * 결과보고 내용 DTO 생성
     *
     * @param deployment Deployment 엔티티
     * @param buildRun BuildRun 엔티티 (배포 실행 정보)
     * @param reportApprovals 결과보고 Approval 리스트
     * @return 결과보고 내용 DTO
     */
    public ReportContentDto buildReportContent(Deployment deployment, BuildRun buildRun, List<Approval> reportApprovals) {
        log.debug("결과보고 내용 생성 - deploymentId: {}, hasReportApproval: {}",
                deployment.getId(), !reportApprovals.isEmpty());

        // ✅ reportApprovals가 없으면 null 반환
        if (reportApprovals.isEmpty()) {
            return null;
        }

        String deploymentResult = null;
        if (deployment.getIsDeployed() != null) {
            deploymentResult = deployment.getIsDeployed() ? "성공" : "실패";
        }

        // ✅ 결과보고서 내용은 Approval의 content에서 가져옴
        Approval reportApproval = reportApprovals.get(0);
        String reportContent = reportApproval.getContent();
        LocalDateTime reportCreatedAt = reportApproval.getCreatedAt();

        return ReportContentDto.builder()
                .deploymentResult(deploymentResult)
                .actualStartedAt(buildRun != null ? formatDateTime(buildRun.getStartedAt()) : null)
                .actualEndedAt(buildRun != null ? formatDateTime(buildRun.getEndedAt()) : null)
                .actualDuration(buildRun != null ? formatDuration(buildRun.getDuration()) : null)
                .reportContent(reportContent)  // ✅ Approval의 content 사용
                .reportCreatedAt(formatDateTime(reportCreatedAt))  // ✅ Approval의 createdAt 사용
                .build();
    }

    /**
     * LocalDateTime을 yyyy.MM.dd HH:mm 형식으로 포맷
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    /**
     * Duration (밀리초)를 "X분 Y초" 형식으로 포맷
     */
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