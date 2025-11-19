package sys.be4man.domains.problem.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sys.be4man.domains.problem.model.entity.Problem;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    List<Problem> findByCategory_IdOrderByIdAsc(Long categoryId);
}
