// 작성자 : 김민호
package sys.be4man.domains.problem.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sys.be4man.domains.problem.model.entity.ProblemCategory;

public interface ProblemCategoryRepository extends JpaRepository<ProblemCategory, Long> {

    List<ProblemCategory> findByProject_IdOrderByIdAsc(Long projectId);
}
