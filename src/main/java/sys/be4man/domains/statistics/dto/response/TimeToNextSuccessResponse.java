// 작성자 : 조윤상
package sys.be4man.domains.statistics.dto.response;

import java.util.List;

public record TimeToNextSuccessResponse(
        long thresholdMins,
        List<TimeToNextSuccessItem> items
) {}