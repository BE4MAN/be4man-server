package sys.be4man.domains.statistics.dto.response;

public record TimeToNextSuccessItem(
        Long projectId,
        String projectName,
        long avgMins,
        long sampleCount,
        long withinMinutes,
        long overMinutes
) {}