package sys.be4man.domains.approval.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.deployment.model.type.DeploymentStage;

public interface ApprovalRepository extends JpaRepository<Approval, Long>, ApprovalRepositoryCustom {

    List<Approval> findByAccountId(Long accountId);
    List<Approval> findByAccountIdAndStatus(Long accountId, ApprovalStatus status);

    @Query("""
        select distinct a
        from Approval a
        left join a.approvalLines l
        where a.account.id = :accountId
           or (a.nextApprover is not null and a.nextApprover.id = :accountId)
           or (l.account.id = :accountId)
        order by a.updatedAt desc
        """)
    List<Approval> findMyApprovals(@Param("accountId") Long accountId);

    @Query("""
        select distinct a
        from Approval a
        left join a.approvalLines l
        where (a.account.id = :accountId
           or (a.nextApprover is not null and a.nextApprover.id = :accountId)
           or (l.account.id = :accountId))
          and a.status = :status
        order by a.updatedAt desc
        """)

    List<Approval> findMyApprovalsByStatus(
            @Param("accountId") Long accountId,
            @Param("status") ApprovalStatus status
    );

    List<Approval> findByDeploymentIdAndTypeOrderByIdAsc(Long taskId, ApprovalType approvalType);
}
