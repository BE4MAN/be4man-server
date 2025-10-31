package sys.be4man.domains.analysis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sys.be4man.domains.analysis.dto.response.BuildResultResponseDto;
import sys.be4man.domains.analysis.dto.response.BuildRunConsoleLogResponseDto;
import sys.be4man.domains.analysis.exception.type.BuildRunExceptionType;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.analysis.repository.BuildRunRepository;
import sys.be4man.global.exception.NotFoundException;

@RequiredArgsConstructor
@Service
public class BuildRunService {

    private final BuildRunRepository buildRunRepository;

    public BuildRunConsoleLogResponseDto getConsoleLogByDeploymentId(Long deploymentId) {
        BuildRun buildRun = buildRunRepository.findByDeploymentIdAndIsDeletedFalse(deploymentId).orElseThrow(
                () -> new NotFoundException(BuildRunExceptionType.BUILD_RUN_NOT_FOUND)
        );

        return BuildRunConsoleLogResponseDto.toDto(deploymentId, buildRun);
    }

    public BuildResultResponseDto getBuildResultByDeploymentId(Long deploymentId) {
        return buildRunRepository.findBuildResultByDeploymentId(deploymentId).orElseThrow();
    }
}
