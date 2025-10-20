package sys.be4man.history.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeployStatus;

import java.time.LocalDateTime;

/**
 * History 페이지용 Repository
 * - 기존 Deployment 엔티티를 사용하여 deployment 테이블 조회
 * - History 화면에 필요한 커스텀 조회 메서드 정의
 */
@Repository
public interface HistoryRepository extends JpaRepository<Deployment, Long> {

    /**
     * 1. 전체 배포 이력 조회 (최신순, 페이징)
     * - History 메인 화면
     * - N+1 문제 방지를 위해 LEFT JOIN FETCH 사용
     */
    @Query("SELECT d FROM Deployment d " +
            "LEFT JOIN FETCH d.project " +
            "LEFT JOIN FETCH d.issuer " +
            "ORDER BY d.createdAt DESC")
    Page<Deployment> findAllHistory(Pageable pageable);

    /**
     * 2. 상태별 필터링 조회
     * - 승인 여부 필터: PENDING, APPROVED, REJECTED, CANCELED
     * - 결과 필터: SUCCESS, FAILURE
     *
     * 사용 예시:
     * - 성공만 보기: findByStatus(DeployStatus.SUCCESS, pageable)
     * - 승인 대기만 보기: findByStatus(DeployStatus.PENDING, pageable)
     */
    @Query("SELECT d FROM Deployment d " +
            "LEFT JOIN FETCH d.project " +
            "LEFT JOIN FETCH d.issuer " +
            "WHERE d.status = :status " +
            "ORDER BY d.createdAt DESC")
    Page<Deployment> findByStatus(
            @Param("status") DeployStatus status,
            Pageable pageable
    );

    /**
     * 3. 프로젝트별 필터링 조회
     * - 특정 프로젝트의 배포 이력만 조회
     */
    @Query("SELECT d FROM Deployment d " +
            "LEFT JOIN FETCH d.project p " +
            "LEFT JOIN FETCH d.issuer " +
            "WHERE p.id = :projectId " +
            "ORDER BY d.createdAt DESC")
    Page<Deployment> findByProjectId(
            @Param("projectId") Long projectId,
            Pageable pageable
    );

    /**
     * 4. 날짜 범위 필터링 조회
     * - 특정 기간의 배포 이력 조회
     *
     * 사용 예시:
     * - 2025년 7월 배포: findByDateRange(
     *     LocalDateTime.of(2025, 7, 1, 0, 0),
     *     LocalDateTime.of(2025, 7, 31, 23, 59),
     *     pageable
     *   )
     */
    @Query("SELECT d FROM Deployment d " +
            "LEFT JOIN FETCH d.project " +
            "LEFT JOIN FETCH d.issuer " +
            "WHERE d.createdAt >= :startDate " +
            "AND d.createdAt <= :endDate " +
            "ORDER BY d.createdAt DESC")
    Page<Deployment> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 5. 복합 필터링 조회 (상태 + 프로젝트 + 날짜)
     * - 모든 조건을 동시에 적용
     * - null인 조건은 무시
     *
     * 사용 예시:
     * - 프로젝트 1의 성공한 배포만: findByFilters(SUCCESS, 1L, null, null, pageable)
     * - 7월의 모든 배포: findByFilters(null, null, startDate, endDate, pageable)
     */
    @Query("SELECT d FROM Deployment d " +
            "LEFT JOIN FETCH d.project p " +
            "LEFT JOIN FETCH d.issuer " +
            "WHERE (:status IS NULL OR d.status = :status) " +
            "AND (:projectId IS NULL OR p.id = :projectId) " +
            "AND (:startDate IS NULL OR d.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR d.createdAt <= :endDate) " +
            "ORDER BY d.createdAt DESC")
    Page<Deployment> findByFilters(
            @Param("status") DeployStatus status,
            @Param("projectId") Long projectId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 6. PR 번호로 검색
     * - 검색창에서 PR 번호 입력 시 사용
     *
     * 사용 예시:
     * - PR #201 검색: findByPrNumber(201, pageable)
     */
    @Query("SELECT d FROM Deployment d " +
            "LEFT JOIN FETCH d.project " +
            "LEFT JOIN FETCH d.issuer " +
            "WHERE d.prNumber = :prNumber " +
            "ORDER BY d.createdAt DESC")
    Page<Deployment> findByPrNumber(
            @Param("prNumber") Integer prNumber,
            Pageable pageable
    );

    /**
     * 7. 브랜치명으로 검색 (부분 일치)
     * - 검색창에서 브랜치명 입력 시 사용
     *
     * 사용 예시:
     * - "feature" 검색: findByBranchContaining("feature", pageable)
     *   → "feature/ui-update", "feature/auth-improvements" 등 조회
     */
    @Query("SELECT d FROM Deployment d " +
            "LEFT JOIN FETCH d.project " +
            "LEFT JOIN FETCH d.issuer " +
            "WHERE d.branch LIKE CONCAT('%', :branch, '%') " +
            "ORDER BY d.createdAt DESC")
    Page<Deployment> findByBranchContaining(
            @Param("branch") String branch,
            Pageable pageable
    );

    /**
     * 8. 특정 배포 상세 조회 (단건)
     * - 브랜치별 상세 화면에서 사용
     */
    @Query("SELECT d FROM Deployment d " +
            "LEFT JOIN FETCH d.project " +
            "LEFT JOIN FETCH d.issuer " +
            "WHERE d.id = :deploymentId")
    Deployment findDetailById(@Param("deploymentId") Long deploymentId);
}