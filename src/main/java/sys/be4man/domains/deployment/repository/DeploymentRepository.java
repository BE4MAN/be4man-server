package sys.be4man.domains.deployment.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sys.be4man.domains.deployment.model.entity.Deployment;

public interface DeploymentRepository extends JpaRepository<Deployment, Long>, DeploymentRepositoryCustom {
    Optional<Deployment> findByIdAndIsDeletedFalse(Long deploymentId);

}