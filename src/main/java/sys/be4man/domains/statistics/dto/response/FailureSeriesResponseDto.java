package sys.be4man.domains.statistics.dto.response;

import java.util.List;
import java.util.Map;

public record FailureSeriesResponseDto(
        Long projectId,
        Summary summary,
        Map<String, List<SeriesPointResponseDto>> series
) {
    public record Summary(Long total, Map<String, Long> typeCounts) {}
}