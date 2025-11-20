package sys.be4man.domains.analysis.dto.response;

/**
 * Jenkins 웹훅 알림 요청 본문(Body)을 수신하기 위한 DTO
 */

public record JenkinsWebhooksResponseDto(
        Long deploymentId,
        // Jenkins 작업 이름
        String jobName,
        // Jenkins 빌드 넘버
        String buildNumber,
        // 빌드 실행 결과 (SUCCESS, FAILURE, UNSTABLE, ABORTED, NOT_BUILT)
        String result,
        // 빌드 수행 시간 (예: 1 min 55 sec and counting)
        String duration,
        // 빌드 시작 시간
        String startTime,
        // 빌드 종료 시간
        String endTime
) {

}
