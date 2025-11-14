package sys.be4man.domains.statistics.controller;


import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sys.be4man.domains.statistics.dto.response.DeployDurationResponse;
import sys.be4man.domains.statistics.dto.response.DeploySuccessRateResponseDto;
import sys.be4man.domains.statistics.dto.response.FailureSeriesResponseDto;
import sys.be4man.domains.statistics.dto.response.PeriodStatsResponse;
import sys.be4man.domains.statistics.service.StatisticsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 예) /api/projects/42/deploy-failures/stats
     * 예) /api/projects/42/deploy-failures/stats?from=2025-01-01&to=2025-11-30
     */
    @GetMapping("/{projectId}/deploy-failures/series")
    public ResponseEntity<FailureSeriesResponseDto> series(
            @PathVariable Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(statisticsService.getSeries(projectId, from, to));
    }

    @GetMapping("/deploy-success-rate")
    public ResponseEntity<DeploySuccessRateResponseDto> getDeploySuccessRate() {
        DeploySuccessRateResponseDto body = statisticsService.getDeploySuccessRate();
        return ResponseEntity.ok(body);
    }



    @GetMapping("/deploy-duration")
    public ResponseEntity<DeployDurationResponse> getDeployDuration(
            @RequestParam(value = "service", required = false, defaultValue = "all") String service
    ) {
        return ResponseEntity.ok(statisticsService.getDeployDuration(service));
    }

    @GetMapping("/period")
    public ResponseEntity<PeriodStatsResponse> getPeriodStats(
            @RequestParam String period,
            @RequestParam(required = false) Long projectId
    ) {
        PeriodStatsResponse body = statisticsService.getPeriodStats(period, projectId);
        return ResponseEntity.ok(body);
    }
}
