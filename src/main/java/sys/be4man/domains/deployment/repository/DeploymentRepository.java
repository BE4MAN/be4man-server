package sys.be4man.domains.deployment.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sys.be4man.domains.deployment.model.entity.Deployment;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    Optional<Deployment> findByIdAndIsDeletedFalse(Long deploymentId);

}