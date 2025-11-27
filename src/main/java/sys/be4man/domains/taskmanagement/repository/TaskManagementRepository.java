// 작성자 : 허겸
package sys.be4man.domains.taskmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.deployment.model.entity.Deployment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 작업 관리 페이지용 Repository
 * - Deployment 엔티티를 사용하여 작업 관리 내역 조회
 */
@Repository
public interface TaskManagementRepository extends JpaRepository<Deployment, Long>, TaskManagementRepositoryCustom {

    /**
     * 특정 배포가 속한 PLAN 찾기
     *
     * 규칙: 현재 배포보다 이전 또는 같은 시간에 생성된 PLAN 중 가장 최근 것
     * 동시 생성 시: ID가 큰 것 우선 (최신 ID)
     *
     * @param projectId 프로젝트 ID
     * @param prId Pull Request ID
     * @param currentTime 현재 배포의 생성 시간
     * @param currentId 현재 배포의 ID (동시 생성 대비)
     * @return 관련 PLAN 배포
     */
    @Query("SELECT d FROM Deployment d " +
           "WHERE d.stage = sys.be4man.domains.deployment.model.type.DeploymentStage.PLAN " +
           "AND d.project.id = :projectId " +
           "AND d.pullRequest.id = :prId " +
           "AND (d.createdAt < :currentTime OR (d.createdAt = :currentTime AND d.id <= :currentId)) " +
           "AND d.isDeleted = false " +
           "ORDER BY d.createdAt DESC, d.id DESC")
    List<Deployment> findRelatedPlanList(
        @Param("projectId") Long projectId,
        @Param("prId") Long prId,
        @Param("currentTime") LocalDateTime currentTime,
        @Param("currentId") Long currentId
    );

    /**
     * 다음 배포 프로세스의 PLAN 찾기
     *
     * 규칙: 현재 PLAN 이후 생성된 PLAN 중 가장 빠른 것
     *
     * @param projectId 프로젝트 ID
     * @param prId Pull Request ID
     * @param planTime 현재 PLAN의 생성 시간
     * @param planId 현재 PLAN의 ID (동시 생성 대비)
     * @return 다음 PLAN (없으면 Optional.empty())
     */
    @Query("SELECT d FROM Deployment d " +
           "WHERE d.stage = sys.be4man.domains.deployment.model.type.DeploymentStage.PLAN " +
           "AND d.project.id = :projectId " +
           "AND d.pullRequest.id = :prId " +
           "AND (d.createdAt > :planTime OR (d.createdAt = :planTime AND d.id > :planId)) " +
           "AND d.isDeleted = false " +
           "ORDER BY d.createdAt ASC, d.id ASC")
    List<Deployment> findNextPlanList(
        @Param("projectId") Long projectId,
        @Param("prId") Long prId,
        @Param("planTime") LocalDateTime planTime,
        @Param("planId") Long planId
    );

}
