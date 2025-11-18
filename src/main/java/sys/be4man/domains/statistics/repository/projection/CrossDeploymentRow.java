package sys.be4man.domains.statistics.repository.projection;

import java.time.LocalDateTime;

public record CrossDeploymentRow(
        Long projectId,
        String projectName,
        Long pullRequestId,
        LocalDateTime startedAt,  // 해당 배포(deployment)의 가장 이른 build_run.started_at
        Boolean isDeployed        // deployment.isDeployed (true/false만 유효, null 제외)
) {}