package sys.be4man.domains.statistics.repository;

import static com.querydsl.core.types.ExpressionUtils.count;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.analysis.model.entity.QBuildRun;
import sys.be4man.domains.analysis.model.entity.QStageRun;
import sys.be4man.domains.deployment.model.entity.QDeployment;
import sys.be4man.domains.project.model.entity.QProject;
import sys.be4man.domains.statistics.dto.response.TypeCountResponseDto;
import sys.be4man.domains.statistics.repository.projection.MonthBucket;
import sys.be4man.domains.statistics.repository.projection.ProjectLight;
import sys.be4man.domains.statistics.repository.projection.ProjectSuccessCount;
import sys.be4man.domains.statistics.repository.projection.TotalSuccessCount;
import sys.be4man.domains.statistics.repository.projection.YearBucket;

@RequiredArgsConstructor
@Repository
public class StatisticsRepositoryCustomImpl implements StatisticsRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    private final QStageRun stageRun = QStageRun.stageRun;
    private final QBuildRun buildRun = QBuildRun.buildRun;
    private static final QBuildRun buildRun2 = new QBuildRun("buildRun2");
    private final QDeployment deployment = QDeployment.deployment;
    private final QProject project = QProject.project;

    @Override
    public List<TypeCountResponseDto> countByProblemTypeForProject(
            Long projectId, LocalDateTime from, LocalDateTime to) {

        QStageRun s = stageRun;
        QBuildRun b = buildRun;
        QDeployment d = deployment;

        return jpaQueryFactory.select(
                        Projections.constructor(
                                TypeCountResponseDto.class,
                                s.problemType.stringValue(), // enum -> String
                                count(s.id)
                        )
                )
                .from(s)
                .join(s.buildRun, b)
                .join(b.deployment, d)
                .where(
                        d.project.id.eq(projectId),
                        s.isSuccess.isFalse(),
                        s.problemType.isNotNull(),
                        betweenOrNull(b.startedAt, from, to)
                )
                .groupBy(s.problemType)
                .fetch();
    }

    @Override
    public List<MonthlyTypeCountRow> monthlySeriesAllTypes(
            Long projectId, LocalDateTime from, LocalDateTime to) {

        QStageRun s = stageRun;
        QBuildRun b = buildRun;
        QDeployment d = deployment;

        // month label: "YYYY-MM"
        var monthLabel = Expressions.stringTemplate(
                "to_char(date_trunc('month', {0}), 'YYYY-MM')", b.startedAt);

        // group key for ordering: date_trunc('month', started_at)
        DateTemplate<LocalDateTime> monthKey = Expressions.dateTemplate(
                LocalDateTime.class, "date_trunc('month', {0})", b.startedAt);

        return jpaQueryFactory.select(
                        Projections.constructor(
                                MonthlyTypeCountRow.class,
                                s.problemType.stringValue(), // String
                                monthLabel,                  // "YYYY-MM"
                                count(s.id)
                        )
                )
                .from(s)
                .join(s.buildRun, b)
                .join(b.deployment, d)
                .where(
                        d.project.id.eq(projectId),
                        s.isSuccess.isFalse(),
                        s.problemType.isNotNull(),
                        betweenOrNull(b.startedAt, from, to)
                )
                .groupBy(s.problemType, monthKey)
                .orderBy(monthKey.asc())
                .fetch();
    }

    private BooleanExpression betweenOrNull(
            com.querydsl.core.types.dsl.DateTimePath<LocalDateTime> path,
            LocalDateTime from, LocalDateTime to
    ) {
        BooleanExpression ge = (from == null) ? null : path.goe(from);
        BooleanExpression lt = (to == null) ? null : path.lt(to);
        if (ge == null) {
            return lt;
        }
        if (lt == null) {
            return ge;
        }
        return ge.and(lt);
    }

    /**
     * 프로젝트가 하나도 배포가 없어도 반드시 결과에 포함되도록 FROM project LEFT JOIN deployment ... 로 변경. 성공/실패는
     * is_deployed = true/false만 집계, null은 제외.
     */
    @Override
    public List<ProjectSuccessCount> findProjectSuccessCounts() {

        // sum(case when deployment.is_deployed = true then 1 else 0 end)
        NumberExpression<Long> successSum = new CaseBuilder()
                .when(deployment.isDeployed.isTrue()).then(1L)
                .otherwise(0L)
                .sum();

        // sum(case when deployment.is_deployed = false then 1 else 0 end)
        NumberExpression<Long> failedSum = new CaseBuilder()
                .when(deployment.isDeployed.isFalse()).then(1L)
                .otherwise(0L)
                .sum();

        return jpaQueryFactory
                .select(Projections.constructor(
                        ProjectSuccessCount.class,
                        project.id,        // projectId
                        project.name,      // projectName
                        // 일부 DB/드라이버에서 모두 0이면 null일 수 있어 coalesce로 방어
                        successSum.coalesce(0L),
                        failedSum.coalesce(0L)
                ))
                .from(project)
                // ✅ LEFT JOIN: 배포가 없는 프로젝트도 결과에 포함
                .leftJoin(deployment)
                .on(
                        deployment.project.id.eq(project.id),
                        // 배포 soft delete 제외 (deployment가 null일 수 있으므로 ON 절에 둠)
                        deployment.isDeleted.isFalse()
                )
                // 프로젝트 soft delete 제외
                .where(project.isDeleted.isFalse())
                .groupBy(project.id, project.name)
                .fetch();
    }

    /**
     * 전체 합계는 모든 프로젝트를 가리지 않고 배포 기준으로 집계. (배포가 없는 프로젝트는 합계에 영향이 없으므로 기존 방식 유지)
     */
    @Override
    public TotalSuccessCount findTotalSuccessCounts() {

        NumberExpression<Long> successSum = new CaseBuilder()
                .when(deployment.isDeployed.isTrue()).then(1L)
                .otherwise(0L)
                .sum();

        NumberExpression<Long> failedSum = new CaseBuilder()
                .when(deployment.isDeployed.isFalse()).then(1L)
                .otherwise(0L)
                .sum();

        return jpaQueryFactory
                .select(Projections.constructor(
                        TotalSuccessCount.class,
                        successSum.coalesce(0L),
                        failedSum.coalesce(0L)
                ))
                .from(deployment)
                .join(project).on(deployment.project.id.eq(project.id))
                .where(
                        project.isDeleted.isFalse(),
                        deployment.isDeleted.isFalse()
                )
                .fetchOne();
    }

    @Override
    public Map<YearMonth, Double> findMonthlyAvgDuration(Long projectId, String projectName,
            LocalDate startInclusive, LocalDate endExclusive) {

        // PostgreSQL: date_trunc('month', timestamp)
        DateTimeExpression<LocalDateTime> monthTrunc =
                Expressions.dateTimeTemplate(java.time.LocalDateTime.class,
                        "date_trunc('month', {0})", buildRun.startedAt);

        List<Tuple> rows = jpaQueryFactory
                .select(monthTrunc, buildRun.duration.avg()) // 평균(초)
                .from(buildRun)
                .join(deployment).on(buildRun.deployment.id.eq(deployment.id)
                        .and(buildRun.isDeleted.isFalse())
                        .and(deployment.isDeleted.isFalse()))
                .join(project).on(deployment.project.id.eq(project.id)
                        .and(project.isDeleted.isFalse()))
                .where(
                        buildRun.startedAt.goe(startInclusive.atStartOfDay()),
                        buildRun.startedAt.lt(endExclusive.atStartOfDay()),
                        // service 필터: id 또는 name
                        projectId != null ? project.id.eq(projectId) : null,
                        projectName != null ? project.name.eq(projectName) : null
                )
                .groupBy(monthTrunc)
                .orderBy(monthTrunc.asc())
                .fetch();

        Map<YearMonth, Double> map = new HashMap<>();
        for (Tuple t : rows) {
            java.time.LocalDateTime monthStart = t.get(0, java.time.LocalDateTime.class);
            Double avgSec = t.get(1, Double.class);
            if (monthStart != null && avgSec != null) {
                YearMonth ym = YearMonth.from(
                        monthStart.atZone(ZoneId.systemDefault()).toLocalDate());
                map.put(ym, avgSec);
            }
        }
        return map;
    }

    @Override
    public List<ProjectLight> findAllProjects() {
        return jpaQueryFactory
                .select(Projections.constructor(
                        ProjectLight.class,
                        project.id,
                        project.name
                ))
                .from(project)
                .where(project.isDeleted.isFalse())
                .orderBy(project.name.asc())
                .fetch();
    }

    @Override
    public List<MonthBucket> findMonthlyDeploymentFinalStats(Long projectId) {
        NumberExpression<Integer> monthExpr =
                Expressions.numberTemplate(Integer.class, "EXTRACT(MONTH FROM {0})", buildRun.endedAt);

        NumberExpression<Long> successCase = new CaseBuilder()
                .when(deployment.isDeployed.isTrue()).then(1L).otherwise(0L);

        NumberExpression<Long> failedCase = new CaseBuilder()
                .when(deployment.isDeployed.isFalse()).then(1L).otherwise(0L);

        SubQueryExpression<java.time.LocalDateTime> maxEndedSub =
                JPAExpressions.select(buildRun2.endedAt.max())
                        .from(buildRun2)
                        .where(buildRun2.deployment.id.eq(deployment.id));

        BooleanExpression deployedKnown = deployment.isDeployed.isNotNull();
        BooleanExpression projectFilter = (projectId == null) ? null : deployment.project.id.eq(projectId);

        return jpaQueryFactory
                .select(Projections.constructor(
                        MonthBucket.class,
                        monthExpr,                   // Integer
                        buildRun.id.count(),         // Long
                        successCase.sum(),           // Long
                        failedCase.sum()             // Long
                ))
                .from(deployment)
                .join(buildRun).on(buildRun.deployment.id.eq(deployment.id))
                .where(
                        buildRun.endedAt.eq(maxEndedSub),
                        deployedKnown,
                        deployment.isDeleted.isFalse(),
                        buildRun.isDeleted.isFalse(),
                        projectFilter
                )
                .groupBy(monthExpr)
                .orderBy(monthExpr.asc())
                .fetch();
    }


    @Override
    public List<YearBucket> findYearlyDeploymentFinalStats(Long projectId) {
        NumberExpression<Integer> yearExpr =
                Expressions.numberTemplate(Integer.class, "EXTRACT(YEAR FROM {0})", buildRun.endedAt);

        NumberExpression<Long> successCase = new CaseBuilder()
                .when(deployment.isDeployed.isTrue()).then(1L).otherwise(0L);

        NumberExpression<Long> failedCase = new CaseBuilder()
                .when(deployment.isDeployed.isFalse()).then(1L).otherwise(0L);

        SubQueryExpression<java.time.LocalDateTime> maxEndedSub =
                JPAExpressions.select(buildRun2.endedAt.max())
                        .from(buildRun2)
                        .where(buildRun2.deployment.id.eq(deployment.id));

        BooleanExpression deployedKnown = deployment.isDeployed.isNotNull();
        BooleanExpression projectFilter = (projectId == null) ? null : deployment.project.id.eq(projectId);

        return jpaQueryFactory
                .select(Projections.constructor(
                        YearBucket.class,
                        yearExpr,                    // Integer
                        buildRun.id.count(),         // Long
                        successCase.sum(),           // Long
                        failedCase.sum()             // Long
                ))
                .from(deployment)
                .join(buildRun).on(buildRun.deployment.id.eq(deployment.id))
                .where(
                        buildRun.endedAt.eq(maxEndedSub),
                        deployedKnown,
                        deployment.isDeleted.isFalse(),
                        buildRun.isDeleted.isFalse(),
                        projectFilter
                )
                .groupBy(yearExpr)
                .orderBy(yearExpr.asc())
                .fetch();
    }
}

