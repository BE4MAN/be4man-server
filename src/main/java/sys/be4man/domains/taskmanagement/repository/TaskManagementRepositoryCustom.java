package sys.be4man.domains.taskmanagement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.taskmanagement.dto.TaskManagementSearchDto;

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
}
