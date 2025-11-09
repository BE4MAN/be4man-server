package sys.be4man.domains.analysis.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sys.be4man.domains.analysis.dto.response.BuildResultResponseDto;
import sys.be4man.domains.analysis.service.BuildRunService;

/**
 * 빌드 결과 컨트롤러
 */
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Console Log", description = "빌드 결과 관련 API")
@RequestMapping("/api/build-result")
@RestController()
public class BuildResultController {

    private final BuildRunService buildRunService;

    @GetMapping("/{deploymentId}")
    public ResponseEntity<List<BuildResultResponseDto>> getAllBuildResultsByDeploymentId(@PathVariable Long deploymentId){
        return ResponseEntity.ok(buildRunService.getAllBuildResultsByDeploymentId(deploymentId));
    }

    @GetMapping("/{deploymentId}/{buildRunId}")
    public ResponseEntity<BuildResultResponseDto> getBuildResultByDeploymentIdAndBuildRunId(@PathVariable Long deploymentId,
            @PathVariable Long buildRunId){
        return ResponseEntity.ok(buildRunService.getBuildResultByDeploymentIdAndBuildRunId(deploymentId, buildRunId));
    }

}
