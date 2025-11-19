package sys.be4man.domains.problem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sys.be4man.domains.problem.model.entity.ProblemDeployment;

public interface ProblemDeploymentRepository extends JpaRepository<ProblemDeployment, Long> {
}
