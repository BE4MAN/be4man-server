package sys.be4man.domains.taskmanagement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.taskmanagement.dto.TaskManagementSearchDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 작업 관리 페이지용 Custom Repository
 * - 복잡한 검색/필터링 쿼리 처리
 */
public interface TaskManagementRepositoryCustom {

    /**
     * 검색 및 필터 조건에 따른 작업 목록 조회
     *
     * @param searchDto 검색/필터 조건
     * @param pageable 페이징 정보
     * @return 페이징된 작업 목록
     */
    Page<Deployment> findTasksBySearchConditions(TaskManagementSearchDto searchDto, Pageable pageable);

    /**
     * 특정 프로세스 범위 내의 모든 배포 찾기 (QueryDSL)
     *
     * @param projectId 프로젝트 ID
     * @param prId Pull Request ID
     * @param startTime 프로세스 시작 시간
     * @param startId 프로세스 시작 ID
     * @param endTime 프로세스 종료 시간 (nullable)
     * @param endId 프로세스 종료 ID (nullable)
     * @return 프로세스에 속한 모든 배포 리스트
     */
    List<Deployment> findProcessDeploymentsQueryDsl(
            Long projectId,
            Long prId,
            LocalDateTime startTime,
            Long startId,
            LocalDateTime endTime,
            Long endId
    );
}
