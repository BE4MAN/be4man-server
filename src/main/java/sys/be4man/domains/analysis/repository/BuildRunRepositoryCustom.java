package sys.be4man.domains.analysis.repository;

import java.util.Optional;
import sys.be4man.domains.analysis.dto.response.BuildResultResponseDto;

public interface BuildRunRepositoryCustom {
    Optional<BuildResultResponseDto> findBuildResultByDeploymentId(Long deploymentId);
}
