package sys.be4man.domains.deployment.repository;

import java.time.LocalDateTime;
import java.util.List;
import sys.be4man.domains.deployment.model.entity.Deployment;

public interface DeploymentRepositoryCustom {

    /**
     * 스케줄된 배포 작업 목록 조회
     * - scheduledAt이 startDateTime ~ endDateTime 범위 내
     * - 삭제되지 않은 것만
     * - PLAN 단계의 REJECTED 상태 제외
     * - DEPLOYMENT 단계의 CANCELED 상태 제외
     */
    List<Deployment> findScheduledDeployments(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    /**
     * 특정 시간 범위와 겹치는 배포 작업 목록 조회
     * - Ban 생성 시 충돌하는 Deployment를 찾기 위한 메서드
     * - scheduledAt 또는 scheduledToEndedAt이 banStartDateTime ~ banEndDateTime 범위와 겹치는 Deployment 조회
     * - 삭제되지 않고, 취소되지 않았으며, 완료되지 않은 Deployment만 조회
     * - 특정 프로젝트 목록에 해당하는 Deployment만 조회
     */
    List<Deployment> findOverlappingDeployments(
            LocalDateTime banStartDateTime,
            LocalDateTime banEndDateTime,
            List<Long> projectIds
    );
}
