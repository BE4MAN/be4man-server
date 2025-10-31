package sys.be4man.domains.analysis.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sys.be4man.domains.analysis.model.entity.BuildRun;

public interface BuildRunRepository extends JpaRepository<BuildRun, Long>, BuildRunRepositoryCustom {

    Optional<BuildRun> findByJenkinsJobNameAndBuildNumberAndIsDeletedFalse(String jenkinsJobName,
            Long buildNumber);

    Optional<BuildRun> findByDeploymentIdAndIsDeletedFalse(Long deploymentId);
}
