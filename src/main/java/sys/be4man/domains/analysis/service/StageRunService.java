package sys.be4man.domains.analysis.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sys.be4man.domains.analysis.dto.response.StageRunResponseDto;
import sys.be4man.domains.analysis.repository.StageRunRepository;

@Slf4j
@RequiredArgsConstructor
@Service
public class StageRunService {

    private final StageRunRepository stageRunRepository;
    public List<StageRunResponseDto> getAllStageRunsByDeploymentId(Long deploymentId) {
        return stageRunRepository.findAllStageRunsByDeploymentId(deploymentId);
    }
}
