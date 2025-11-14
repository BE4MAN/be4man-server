package sys.be4man.domains.statistics.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import sys.be4man.domains.statistics.dto.response.TypeCountResponseDto;
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
    // 반환은 (problemType + month + count) "행" 목록으로 받고, Service에서 12개월 라벨로 정렬/0채움
    List<MonthlyTypeCountRow> monthlySeriesAllTypes(
            Long projectId, LocalDateTime from, LocalDateTime to);

    // 쿼리 결과를 담을 최소 레코드(문자열 month로 라벨 생성)
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
    List<YearBucket>  findYearlyDeploymentFinalStats(Long projectId);
}
