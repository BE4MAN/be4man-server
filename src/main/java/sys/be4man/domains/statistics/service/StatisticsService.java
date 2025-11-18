package sys.be4man.domains.statistics.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.analysis.model.type.ProblemType;
import sys.be4man.domains.statistics.dto.response.BanTypeStatsResponse;
import sys.be4man.domains.statistics.dto.response.DeployDurationResponse;
import sys.be4man.domains.statistics.dto.response.DeploySuccessRateResponseDto;
import sys.be4man.domains.statistics.dto.response.DeploySuccessRateResponseDto.Count;
import sys.be4man.domains.statistics.dto.response.DeploySuccessRateResponseDto.ServiceRate;
import sys.be4man.domains.statistics.dto.response.FailureSeriesResponseDto;
import sys.be4man.domains.statistics.dto.response.FailureSeriesResponseDto.Summary;
import sys.be4man.domains.statistics.dto.response.MonthDurationDto;
import sys.be4man.domains.statistics.dto.response.PeriodStatsResponse;
import sys.be4man.domains.statistics.dto.response.SeriesPointResponseDto;
import sys.be4man.domains.statistics.dto.response.ServiceOptionDto;
import sys.be4man.domains.statistics.dto.response.TimeToNextSuccessItem;
import sys.be4man.domains.statistics.dto.response.TimeToNextSuccessResponse;
import sys.be4man.domains.statistics.dto.response.TypeCountResponseDto;
import sys.be4man.domains.statistics.repository.StatisticsRepositoryCustom;
import sys.be4man.domains.statistics.repository.projection.CrossDeploymentRow;
import sys.be4man.domains.statistics.repository.projection.IntraBuildRow;
import sys.be4man.domains.statistics.repository.projection.MonthBucket;
import sys.be4man.domains.statistics.repository.projection.ProjectSuccessCount;
import sys.be4man.domains.statistics.repository.projection.TotalSuccessCount;
import sys.be4man.domains.statistics.repository.projection.YearBucket;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsRepositoryCustom statisticsRepository;

    public FailureSeriesResponseDto getSeries(String serviceId, LocalDate from, LocalDate to) {
        // serviceId 파싱
        // null 또는 "all" -> projectId = null (전체)
        // 숫자 문자열 -> 해당 projectId
        Long projectId = null;
        if (!isAll(serviceId)) {
            projectId = parseLongOrNull(serviceId);
        }

        // 시간 경계 (to는 exclusive)
        LocalDateTime fromTs = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toTs = (to == null) ? null : to.plusDays(1).atStartOfDay();

        // 1) summary: 유형별 총합
        List<TypeCountResponseDto> typeCountsRows =
                statisticsRepository.countByProblemTypeForProject(projectId, fromTs, toTs);

        Map<String, Long> typeCounts = new LinkedHashMap<>();
        long total = 0L;

        // 모든 ProblemType을 0으로 먼저 깔아두기 (누락 방지)
        for (ProblemType pt : ProblemType.values()) {
            typeCounts.put(pt.name(), 0L);
        }

        for (TypeCountResponseDto row : typeCountsRows) {
            typeCounts.put(row.problemType(), row.cnt());
            total += row.cnt();
        }

        // 2) series: (유형, month) → count
        var rawRows = statisticsRepository.monthlySeriesAllTypes(projectId, fromTs, toTs);

        // 2-1) 라벨(YYYY-MM) 만들기
        List<String> labels = buildMonthLabels(from, to);

        // 2-2) 유형 리스트 (ProblemType + "ALL")
        List<String> keys = Arrays.stream(ProblemType.values())
                .map(Enum::name)
                .collect(Collectors.toCollection(ArrayList::new));
        keys.add("ALL");

        // 2-3) 초기화: 모든 유형에 대해 모든 month 0
        Map<String, List<SeriesPointResponseDto>> series = new LinkedHashMap<>();
        for (String k : keys) {
            series.put(k, labels.stream()
                    .map(m -> new SeriesPointResponseDto(m, 0L))
                    .collect(Collectors.toCollection(ArrayList::new)));
        }

        // 2-4) 값 채우기: 유형별 해당 month index 찾아서 count 반영
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) {
            indexMap.put(labels.get(i), i);
        }

        for (var row : rawRows) {
            String type = row.problemType();
            String month = row.month();
            Long cnt = row.count();
            Integer idx = indexMap.get(month);
            if (idx == null) {
                continue; // 방어
            }
            var list = series.get(type);
            if (list == null) {
                continue; // 방어(알 수 없는 타입)
            }
            var old = list.get(idx);
            list.set(idx, new SeriesPointResponseDto(month, old.count() + cnt));
        }

        // 2-5) ALL = 모든 유형 합계
        for (int i = 0; i < labels.size(); i++) {
            long sum = 0;
            for (ProblemType pt : ProblemType.values()) {
                sum += series.get(pt.name()).get(i).count();
            }
            series.get("ALL").set(i, new SeriesPointResponseDto(labels.get(i), sum));
        }

        // projectId는 선택적으로만 채워짐 (전체일 때는 null)
        return new FailureSeriesResponseDto(
                projectId,
                new FailureSeriesResponseDto.Summary(total, typeCounts),
                series
        );
    }

    // from/to 없으면 최근 12개월(현재 포함) 라벨 생성
    private List<String> buildMonthLabels(LocalDate from, LocalDate to) {
        List<String> labels = new ArrayList<>();

        if (from == null && to == null) {
            YearMonth ym = YearMonth.now().minusMonths(11);
            for (int i = 0; i < 12; i++) {
                labels.add(ym.toString()); // YYYY-MM
                ym = ym.plusMonths(1);
            }
            return labels;
        }

        LocalDate start =
                (from != null) ? from.withDayOfMonth(1) : LocalDate.now().withDayOfMonth(1);
        LocalDate endExclusive =
                (to != null ? to.plusDays(1) : LocalDate.now().plusMonths(1)).withDayOfMonth(1);

        YearMonth cur = YearMonth.from(start);
        YearMonth end = YearMonth.from(endExclusive);
        while (!cur.equals(end)) {
            labels.add(cur.toString());
            cur = cur.plusMonths(1);
        }
        return labels;
    }

    public DeploySuccessRateResponseDto getDeploySuccessRate() {
        // 프로젝트별 성공/실패 건수 조회
        List<ProjectSuccessCount> perProject = statisticsRepository.findProjectSuccessCounts();

        // 전체 합계(모든 프로젝트) 조회
        TotalSuccessCount total = statisticsRepository.findTotalSuccessCounts();

        // DTO로 변환
        List<ServiceRate> services = perProject.stream()
                .map(p -> new ServiceRate(p.projectId(), p.projectName(),
                        safeLong(p.success()), safeLong(p.failed())))
                .toList();

        Count all = new Count(
                safeLong(total != null ? total.success() : 0L),
                safeLong(total != null ? total.failed() : 0L)
        );

        return new DeploySuccessRateResponseDto(services, all);
    }

    private long safeLong(Long v) {
        return v == null ? 0L : v;
    }

    public DeployDurationResponse getDeployDuration(String service) {
        // 최근 12개월 범위 계산 (현재월 포함)
        YearMonth endYm = YearMonth.now();
        YearMonth startYm = endYm.minusMonths(11);

        LocalDate startInclusive = startYm.atDay(1);
        LocalDate endExclusive = endYm.plusMonths(1).atDay(1); // 다음달 1일 (exclusive)

        // service 파싱: 숫자면 projectId, 아니면 projectName 로 간주
        Long projectId = parseLongOrNull(service);
        String projectName =
                (projectId == null && !"all".equalsIgnoreCase(service)) ? service : null;

        // 월별 평균(초)을 조회 → 서비스에서 "월 시퀄 채우기 + 분으로 변환"
        Map<YearMonth, Double> avgSecByMonth = statisticsRepository
                .findMonthlyAvgDuration(projectId, projectName, startInclusive, endExclusive);

        List<MonthDurationDto> months = densifyAndConvertToMinutes(startYm, 12, avgSecByMonth);

        // service=all 이면 서비스 옵션도 함께 제공 (드롭다운용)
        if (isAll(service)) {
            List<ServiceOptionDto> services = statisticsRepository.findAllProjects()
                    .stream()
                    .map(p -> new ServiceOptionDto(String.valueOf(p.id()), p.name()))
                    .sorted(Comparator.comparing(ServiceOptionDto::name))
                    .collect(Collectors.toList());

            // '전체' 옵션은 프론트에서 prepend 하므로 여기선 전체 프로젝트만
            return DeployDurationResponse.builder()
                    .months(months)
                    .services(services)
                    .build();
        }

        return DeployDurationResponse.onlyMonths(months);
    }

    private static boolean isAll(String s) {
        return s == null || "all".equalsIgnoreCase(s.trim());
    }

    private static Long parseLongOrNull(String s) {
        try {
            if (s == null) {
                return null;
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 월 시퀀스를 12칸으로 고정하고, 누락된 달은 0분으로 채움. 초 → 분(double) 변환 시 60.0으로 나눔.
     */
    private static List<MonthDurationDto> densifyAndConvertToMinutes(
            YearMonth startYm, int count, Map<YearMonth, Double> avgSecByMonth
    ) {
        List<MonthDurationDto> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            YearMonth ym = startYm.plusMonths(i);
            double minutes = 0.0;
            Double sec = avgSecByMonth.get(ym);
            if (sec != null) {
                minutes = sec / 60.0; // 초 → 분
            }
            result.add(new MonthDurationDto(ym.toString(), round1(minutes))); // "YYYY-MM"
        }
        return result;
    }

    private static double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    public PeriodStatsResponse getPeriodStats(String period, Long projectId) {
        if ("month".equalsIgnoreCase(period)) {
            return buildMonthResponse(projectId);
        } else if ("year".equalsIgnoreCase(period)) {
            return buildYearResponse(projectId);
        } else {
            throw new IllegalArgumentException("period must be 'month' or 'year'");
        }
    }

    private PeriodStatsResponse buildMonthResponse(Long projectId) {
        List<MonthBucket> raw = statisticsRepository.findMonthlyDeploymentFinalStats(projectId);

        // 1~12월 전체 채우기(없으면 0)
        Map<Integer, MonthBucket> map = raw.stream()
                .collect(Collectors.toMap(MonthBucket::month, m -> m));

        List<PeriodStatsResponse.Item> items = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> {
                    MonthBucket b = map.get(m);
                    long deployments = b != null ? b.deployments() : 0L;
                    long success = b != null ? b.success() : 0L;
                    long failed = b != null ? b.failed() : 0L;
                    return new PeriodStatsResponse.Item(
                            String.valueOf(m), deployments, success, failed
                    );
                })
                .toList();

        return new PeriodStatsResponse("month", projectId, items);
    }

    private PeriodStatsResponse buildYearResponse(Long projectId) {
        List<YearBucket> raw = statisticsRepository.findYearlyDeploymentFinalStats(projectId);

        List<PeriodStatsResponse.Item> items = raw.stream()
                .sorted(Comparator.comparing(YearBucket::year))
                .map(b -> new PeriodStatsResponse.Item(
                        String.valueOf(b.year()),
                        b.deployments(),
                        b.success(),
                        b.failed()
                ))
                .toList();

        return new PeriodStatsResponse("year", projectId, items);
    }

    public BanTypeStatsResponse getBanTypeStats(Long projectId) {
        var rows = statisticsRepository.findBanTypeCounts(projectId);

        long total = rows.stream().mapToLong(r -> r.count()).sum();

        var items = rows.stream()
                .map(r -> new BanTypeStatsResponse.Item(r.type(), r.count()))
                .toList();

        return new BanTypeStatsResponse(projectId, items, total);
    }

    /**
     * 배포 실패 → 다음 배포 성공까지 걸린 시간(분)을 프로젝트별로 집계 - 케이스 A: 같은 deployment에서 재시도 (build_run 내에서 마지막 실패 →
     * 다음 성공) - 케이스 B: 다른 deployment로 재시도 (같은 project+PR 안에서 실패 deployment → 다음 성공 deployment) -
     * "실패만 있고 다음 성공이 아직 없는 경우"는 무시
     */
    @Transactional(readOnly = true)
    public TimeToNextSuccessResponse getTimeToNextSuccessPerProject(Long projectId,
            long thresholdMins) {

        // ── 1) 원시 이벤트 조회
        List<IntraBuildRow> intraRows = statisticsRepository.fetchIntraDeploymentBuildRuns(
                projectId);
        List<CrossDeploymentRow> crossRows = statisticsRepository.fetchCrossDeploymentEvents(
                projectId);

        // ── 2) 케이스 A: 같은 deployment 안에서 "마지막 실패 → 다음 성공"
        Map<Long, ProjectAgg> agg = new LinkedHashMap<>();
        Map<Long, List<IntraBuildRow>> byDeployment = intraRows.stream()
                .collect(Collectors.groupingBy(IntraBuildRow::deploymentId, LinkedHashMap::new,
                        Collectors.toList()));

        for (List<IntraBuildRow> logs : byDeployment.values()) {
            // 정렬 보장(쿼리에서 already ASC), 그래도 혹시 모를 null 방어
            logs = logs.stream()
                    .filter(r -> r.startedAt() != null && r.isBuild() != null)
                    .sorted(Comparator.comparing(IntraBuildRow::startedAt))
                    .toList();
            if (logs.isEmpty()) {
                continue;
            }

            // "가장 최근 실패" 인덱스 찾기
            int lastFail = -1;
            for (int i = logs.size() - 1; i >= 0; i--) {
                if (Boolean.FALSE.equals(logs.get(i).isBuild())) {
                    lastFail = i;
                    break;
                }
            }
            if (lastFail < 0) {
                continue; // 실패 없음 → 스킵
            }

            // 그 다음 성공 찾기
            LocalDateTime failAt = logs.get(lastFail).startedAt();
            LocalDateTime nextSuccessAt = null;
            for (int j = lastFail + 1; j < logs.size(); j++) {
                if (Boolean.TRUE.equals(logs.get(j).isBuild())) {
                    nextSuccessAt = logs.get(j).startedAt();
                    break;
                }
            }
            if (nextSuccessAt == null) {
                continue; // 다음 성공 없음 → 스킵
            }

            long minutes = Duration.between(failAt, nextSuccessAt).toMinutes();
            if (minutes < 0) {
                continue; // 역전 방어
            }

            Long pid = logs.get(0).projectId();
            String pname = logs.get(0).projectName();
            agg.compute(pid, (k, a) -> {
                if (a == null) {
                    a = new ProjectAgg(pname);
                }
                a.samples.add(minutes);
                return a;
            });
        }

        // ── 3) 케이스 B: 다른 deployment로 재시도 (같은 project+PR 안에서)
        record PRKey(Long projectId, Long pullRequestId, String projectName) {

        }
        Map<PRKey, List<CrossDeploymentRow>> byPR = crossRows.stream()
                .collect(Collectors.groupingBy(
                        r -> new PRKey(r.projectId(), r.pullRequestId(), r.projectName()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (Map.Entry<PRKey, List<CrossDeploymentRow>> e : byPR.entrySet()) {
            List<CrossDeploymentRow> seq = e.getValue().stream()
                    .filter(r -> r.startedAt() != null && r.isDeployed() != null)
                    .sorted(Comparator.comparing(CrossDeploymentRow::startedAt))
                    .toList();
            if (seq.isEmpty()) {
                continue;
            }

            // "가장 최근 실패" 찾기
            int lastFail = -1;
            for (int i = seq.size() - 1; i >= 0; i--) {
                if (Boolean.FALSE.equals(seq.get(i).isDeployed())) {
                    lastFail = i;
                    break;
                }
            }
            if (lastFail < 0) {
                continue;
            }

            // 그 다음 성공 찾기
            LocalDateTime failAt = seq.get(lastFail).startedAt();
            LocalDateTime nextSuccessAt = null;
            for (int j = lastFail + 1; j < seq.size(); j++) {
                if (Boolean.TRUE.equals(seq.get(j).isDeployed())) {
                    nextSuccessAt = seq.get(j).startedAt();
                    break;
                }
            }
            if (nextSuccessAt == null) {
                continue; // 다음 성공 없음 → 스킵
            }

            long minutes = Duration.between(failAt, nextSuccessAt).toMinutes();
            if (minutes < 0) {
                continue;
            }

            Long pid = e.getKey().projectId();
            String pname = e.getKey().projectName();
            agg.compute(pid, (k, a) -> {
                if (a == null) {
                    a = new ProjectAgg(pname);
                }
                a.samples.add(minutes);
                return a;
            });
        }

        // ── 4) 프로젝트별 집계 → 응답 아이템
        List<TimeToNextSuccessItem> items = agg.entrySet().stream()
                .map(en -> {
                    Long pid = en.getKey();
                    ProjectAgg a = en.getValue();
                    long cnt = a.samples.size();
                    long sum = a.samples.stream().mapToLong(Long::longValue).sum();
                    long avg = cnt == 0 ? 0 : Math.round((double) sum / cnt);

                    long within = 0, over = 0;
                    for (long m : a.samples) {
                        long w = Math.min(m, thresholdMins);
                        within += w;
                        over += Math.max(0, m - thresholdMins);
                    }
                    return new TimeToNextSuccessItem(pid, a.projectName, avg, cnt, within, over);
                })
                .sorted(Comparator.comparing(TimeToNextSuccessItem::projectId))
                .toList();

        return new TimeToNextSuccessResponse(thresholdMins, items);
    }

    // 내부 누적용
    private static final class ProjectAgg {

        final String projectName;
        final List<Long> samples = new ArrayList<>();

        ProjectAgg(String projectName) {
            this.projectName = projectName;
        }
    }
}
