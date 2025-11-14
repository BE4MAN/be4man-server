package sys.be4man.domains.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.taskmanagement.dto.PlanContentDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 작업 상세 - 작업 정보 및 상세 정보 생성 서비스
 * - 계획서 내용 (PlanContentDto)
 * - Jenkins 로그 (JenkinsLogDto)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetailInfoService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    /**
     * 계획서 내용 DTO 생성
     */
    public PlanContentDto buildPlanContent(Deployment deployment) {
        log.debug("계획서 내용 생성 - deploymentId: {}", deployment.getId());

        return PlanContentDto.builder()
                .drafter(deployment.getIssuer() != null ? deployment.getIssuer().getName() : null)
                .department(deployment.getIssuer() != null && deployment.getIssuer().getDepartment() != null ?
                        deployment.getIssuer().getDepartment().getKoreanName() : null)
                .createdAt(formatDateTime(deployment.getCreatedAt()))
                .serviceName(deployment.getProject() != null ? deployment.getProject().getName() : null)
                .taskTitle(deployment.getTitle())
                .scheduledAt(formatDateTime(deployment.getScheduledAt()))
                .scheduledToEndedAt(formatDateTime(deployment.getScheduledToEndedAt()))
                .riskDescription(null)  // Deployment에 없는 필드
                .expectedDuration(deployment.getExpectedDuration())
                .version(deployment.getVersion())
                .strategy(null)  // Deployment에 없는 필드
                .content(deployment.getContent())
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