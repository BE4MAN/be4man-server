package sys.be4man.domains.problem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.problem.model.entity.ProblemDeployment;

public interface ProblemDeploymentRepository extends JpaRepository<ProblemDeployment, Long> {
    @Modifying
    @Transactional
    void deleteAllByProblem_Id(Long problemId);
}
