package sys.be4man.domains.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.taskmanagement.dto.ReportContentDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
     */
    public ReportContentDto buildReportContent(Deployment deployment, BuildRun buildRun) {
        log.debug("결과보고 내용 생성 - deploymentId: {}", deployment.getId());

        if (deployment.getStage() != DeploymentStage.REPORT) {
            return null;
        }

        String deploymentResult = null;
        if (deployment.getIsDeployed() != null) {
            deploymentResult = deployment.getIsDeployed() ? "성공" : "실패";
        }

        return ReportContentDto.builder()
                .deploymentResult(deploymentResult)
                .actualStartedAt(buildRun != null ? formatDateTime(buildRun.getStartedAt()) : null)
                .actualEndedAt(buildRun != null ? formatDateTime(buildRun.getEndedAt()) : null)
                .actualDuration(buildRun != null ? formatDuration(buildRun.getDuration()) : null)
                .reportContent(deployment.getContent())
                .reportCreatedAt(formatDateTime(deployment.getUpdatedAt()))
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