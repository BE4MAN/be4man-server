package sys.be4man.domains.deployment.repository;

import java.time.LocalDateTime;
import java.util.List;
import sys.be4man.domains.deployment.model.entity.Deployment;

public interface DeploymentRepositoryCustom {

    /**
     * 스케줄된 배포 작업 목록 조회
     * - scheduledAt이 startDateTime ~ endDateTime 범위 내
     * - 삭제되지 않은 것만
     */
    List<Deployment> findScheduledDeployments(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}
