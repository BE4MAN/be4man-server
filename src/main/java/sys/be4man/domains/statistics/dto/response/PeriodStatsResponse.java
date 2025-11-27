// 작성자 : 조윤상
package sys.be4man.domains.statistics.dto.response;

import java.util.List;

public record PeriodStatsResponse(
        String period,            // "month" | "year"
        Long projectId,           // null 가능
        List<Item> items          // 월별 12개 or 연도별 N개
) {
    public record Item(
            String label,         // "1".."12" (month) or "2023" (year)
            long deployments,     // 최종 판정이 존재하는 배포 수(= success + failed)
            long success,         // is_deployed = true
            long failed           // is_deployed = false
    ) {}
}