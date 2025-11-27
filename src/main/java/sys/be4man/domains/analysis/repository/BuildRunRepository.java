// 작성자 : 허겸, 이원석
package sys.be4man.domains.analysis.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sys.be4man.domains.analysis.model.entity.BuildRun;

public interface BuildRunRepository extends JpaRepository<BuildRun, Long>, BuildRunRepositoryCustom {

    Optional<BuildRun> findByDeploymentIdAndIdAndIsDeletedFalse(Long deploymentId, Long buildRunId);

    Optional<BuildRun> findByDeploymentIdAndIsDeletedFalse(Long taskId);

    List<BuildRun> findByDeploymentIdIn(List<Long> deploymentIds);
}
