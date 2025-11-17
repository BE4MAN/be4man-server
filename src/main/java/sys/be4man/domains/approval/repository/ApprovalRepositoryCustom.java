package sys.be4man.domains.approval.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.taskmanagement.dto.TaskManagementSearchDto;

import java.util.List;

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
}