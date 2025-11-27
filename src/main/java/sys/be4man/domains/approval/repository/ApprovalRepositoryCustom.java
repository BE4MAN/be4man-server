// 작성자 : 허겸, 이원석
package sys.be4man.domains.approval.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.taskmanagement.dto.TaskManagementSearchDto;

public interface ApprovalRepositoryCustom {

    /**
     * @deprecated 작업 목록 조회는 Deployment 테이블에서만 수행
     */
    @Deprecated
    Page<Approval> findApprovalsBySearchConditions(
            TaskManagementSearchDto searchDto,
            Pageable pageable
    );

    /**
     * 특정 Deployment의 Approval 조회 (타입별)
     */
    List<Approval> findByDeploymentIdAndType(Long deploymentId, ApprovalType type);

    /**
     * 특정 Deployment의 모든 Approval 조회
     */
    List<Approval> findByDeploymentId(Long deploymentId);

    /**
     * 승인 대기 중인 Approval 조회
     */
    List<Approval> findPendingApprovalsByApprover(Long accountId);

    /**
     * 복구현황 목록 조회 (ROLLBACK 타입 Approval의 Deployment 조회) - approval.type = 'ROLLBACK' - deployment와
     * 조인 - 정렬: deployment.createdAt DESC - 페이지네이션 지원
     *
     * @param offset   시작 위치
     * @param pageSize 페이지 크기
     * @return 복구현황 Deployment 목록
     */
    List<Deployment> findRollbackDeployments(int offset, int pageSize);

    /**
     * 복구현황 목록 총 개수 조회
     *
     * @return 총 개수
     */
    long countRollbackDeployments();
}