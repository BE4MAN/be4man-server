// 작성자 : 조윤상
package sys.be4man.domains.statistics.dto.response;

import java.util.List;

public record BanTypeStatsResponse(
        Long projectId,
        List<Item> items,   // [{type:"DB_MIGRATION", count: 12}, ...]
        long total          // items.count 합계
) {
    public record Item(String type, long count) {}
}