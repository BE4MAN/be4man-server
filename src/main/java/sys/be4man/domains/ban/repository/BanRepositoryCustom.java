// 작성자 : 이원석
package sys.be4man.domains.ban.repository;

import java.time.LocalDate;
import java.util.List;
import sys.be4man.domains.ban.model.entity.Ban;
import sys.be4man.domains.ban.model.type.BanType;

public interface BanRepositoryCustom {

    /**
     * 작업 금지 기간 목록 조회
     * - 삭제되지 않은 것만
     * - query: 제목 또는 설명에 포함
     * - startDate/endDate: startedAt 또는 endedAt이 범위 내
     * - type: BanType 필터링
     * - projectIds: 연관된 프로젝트 중 하나라도 포함되면 포함
     */
    List<Ban> findBans(
            String query,
            LocalDate startDate,
            LocalDate endDate,
            BanType type,
            List<Long> projectIds
    );
}

