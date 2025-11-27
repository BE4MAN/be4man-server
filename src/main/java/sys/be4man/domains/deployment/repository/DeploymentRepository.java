// 작성자 : 이원석
package sys.be4man.domains.deployment.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;

public interface DeploymentRepository extends JpaRepository<Deployment, Long>, DeploymentRepositoryCustom {
    Optional<Deployment> findByIdAndIsDeletedFalse(Long deploymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Deployment d where d.id = :id")
    Optional<Deployment> findByIdForUpdate(@Param("id") Long id);

    Optional<Deployment> findTopByPullRequest_IdAndStatusOrderByCreatedAtDesc(
            Long pullRequestId,
            DeploymentStatus status
    );

}