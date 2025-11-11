package sys.be4man.domains.approval.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.type.ApprovalStatus;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByAccountId(Long accountId);
    List<Approval> findByAccountIdAndStatus(Long accountId, ApprovalStatus status);
}
