// 작성자 : 조윤상
package sys.be4man.domains.statistics.repository;

import com.querydsl.core.Tuple;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import sys.be4man.domains.statistics.dto.response.TypeCountResponseDto;
import sys.be4man.domains.statistics.repository.projection.CrossDeploymentRow;
import sys.be4man.domains.statistics.repository.projection.IntraBuildRow;
import sys.be4man.domains.statistics.repository.projection.MonthBucket;
import sys.be4man.domains.statistics.repository.projection.ProjectLight;
import sys.be4man.domains.statistics.repository.projection.ProjectSuccessCount;
import sys.be4man.domains.statistics.repository.projection.TotalSuccessCount;
import sys.be4man.domains.statistics.repository.projection.YearBucket;

public interface StatisticsRepositoryCustom {

    // 도넛 차트용: 문제 유형별 총합
    List<TypeCountResponseDto> countByProblemTypeForProject(
            Long projectId, LocalDateTime from, LocalDateTime to);

    // 라인 차트용: (유형, month) 그룹의 월별 카운트 전체(모든 유형)
    List<MonthlyTypeCountRow> monthlySeriesAllTypes(
            Long projectId, LocalDateTime from, LocalDateTime to);

    record MonthlyTypeCountRow(String problemType, String month, Long count) {}

    /**
     * 프로젝트별 성공/실패 개수 집계
     */
    List<ProjectSuccessCount> findProjectSuccessCounts();

    /**
     * 모든 프로젝트 합계(성공/실패) 집계
     */
    TotalSuccessCount findTotalSuccessCounts();

    /**
     * 최근 12개월 월별 평균 소요시간(초)을 조회.
     * key: YearMonth, value: 평균초(double)
     */
    Map<YearMonth, Double> findMonthlyAvgDuration(Long projectId, String projectName,
            LocalDate startInclusive, LocalDate endExclusive);

    /**
     * 드롭다운용 프로젝트 전체 목록 (soft delete 제외)
     */
    List<ProjectLight> findAllProjects();

    List<MonthBucket> findMonthlyDeploymentFinalStats(Long projectId);
    List<YearBucket> findYearlyDeploymentFinalStats(Long projectId);

    List<BanTypeRow> findBanTypeCounts(Long projectId);

    record BanTypeRow(String type, Long count) {}

    List<IntraBuildRow> fetchIntraDeploymentBuildRuns(Long projectId);

    List<CrossDeploymentRow> fetchCrossDeploymentEvents(Long projectId);

}
