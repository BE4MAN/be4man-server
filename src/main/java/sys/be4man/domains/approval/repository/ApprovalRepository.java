package sys.be4man.domains.approval.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.approval.model.type.ApprovalType;

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

    @Query("""
      select a
      from Approval a
      left join fetch a.approvalLines
      where a.id = :id
    """)
    Optional<Approval> findByIdWithLines(@Param("id") Long id);

    List<Approval> findByDeploymentIdAndTypeAndIsDeletedFalse(Long id, ApprovalType approvalType);

    /**
     * Deployment ID로 Approval 조회
     */
    List<Approval> findByDeploymentId(Long deploymentId);
}
