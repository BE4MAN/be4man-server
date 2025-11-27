// 작성자 : 조윤상
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
import sys.be4man.domains.analysis.dto.response.BuildRunConsoleLogResponseDto;
import sys.be4man.domains.analysis.dto.response.StageRunResponseDto;
import sys.be4man.domains.analysis.service.BuildRunService;
import sys.be4man.domains.analysis.service.StageRunService;

/**
 * 콘솔 로그 컨트롤러
 */
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Console Log", description = "젠킨스 콘솔 로그 관련 API")
@RequestMapping("/api/console-log")
@RestController()
public class ConsoleLogController {

    private final BuildRunService buildRunService;
    private final StageRunService stageRunService;

    @GetMapping("/{deploymentId}/{buildRunId}")
    public ResponseEntity<BuildRunConsoleLogResponseDto> getConsoleLogByDeploymentIdAndBuildRunId(
            @PathVariable Long deploymentId, @PathVariable Long buildRunId) {
        return ResponseEntity.ok(
                buildRunService.getConsoleLogByDeploymentIdAndBuildRunId(deploymentId, buildRunId));
    }

    @GetMapping("/{buildRunId}/all-stages")
    public ResponseEntity<List<StageRunResponseDto>> getAllStageRunsByBuildRunId(
            @PathVariable Long buildRunId) {
        return ResponseEntity.ok(
                stageRunService.getAllStageRunsByBuildRunId(buildRunId));
    }
}
