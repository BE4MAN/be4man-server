package sys.be4man.domains.statistics.repository;

import static com.querydsl.core.types.ExpressionUtils.count;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
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
import sys.be4man.domains.ban.model.entity.QBan;
import sys.be4man.domains.ban.model.entity.QProjectBan;
import sys.be4man.domains.deployment.model.entity.QDeployment;
import sys.be4man.domains.project.model.entity.QProject;
import sys.be4man.domains.statistics.dto.response.TypeCountResponseDto;
import sys.be4man.domains.statistics.repository.projection.CrossDeploymentRow;
import sys.be4man.domains.statistics.repository.projection.IntraBuildRow;
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

    private static final QProjectBan projectBan = QProjectBan.projectBan;
    private static final QBan ban = QBan.ban;

    // ------------------------------------------------------------
    // 실패 유형 집계 / 월별 시리즈 / 성공·실패 카운트 / 평균 소요시간 / 프로젝트 목록
    // ------------------------------------------------------------

    @Override
    public List<TypeCountResponseDto> countByProblemTypeForProject(
            Long projectId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return jpaQueryFactory
                .select(Projections.constructor(
                        TypeCountResponseDto.class,
                        stageRun.problemType.stringValue(), // enum -> String
                        count(stageRun.id)
                ))
                .from(stageRun)
                .join(stageRun.buildRun, buildRun)
                .join(buildRun.deployment, deployment)
                .where(
                        projectId != null ? deployment.project.id.eq(projectId) : null,
                        stageRun.isSuccess.isFalse(),
                        stageRun.problemType.isNotNull(),
                        betweenOrNull(buildRun.startedAt, from, to)
                )
                .groupBy(stageRun.problemType)
                .fetch();
    }

    @Override
    public List<MonthlyTypeCountRow> monthlySeriesAllTypes(
            Long projectId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        // month label: "YYYY-MM"
        var monthLabel = Expressions.stringTemplate(
                "to_char(date_trunc('month', {0}), 'YYYY-MM')",
                buildRun.startedAt
        );

        // group key for ordering
        DateTemplate<LocalDateTime> monthKey = Expressions.dateTemplate(
                LocalDateTime.class,
                "date_trunc('month', {0})",
                buildRun.startedAt
        );

        return jpaQueryFactory
                .select(Projections.constructor(
                        MonthlyTypeCountRow.class,
                        stageRun.problemType.stringValue(), // problemType
                        monthLabel,                          // "YYYY-MM"
                        count(stageRun.id)                   // count
                ))
                .from(stageRun)
                .join(stageRun.buildRun, buildRun)
                .join(buildRun.deployment, deployment)
                .where(
                        projectId != null ? deployment.project.id.eq(projectId) : null,
                        stageRun.isSuccess.isFalse(),
                        stageRun.problemType.isNotNull(),
                        betweenOrNull(buildRun.startedAt, from, to)
                )
                .groupBy(stageRun.problemType, monthKey)
                .orderBy(monthKey.asc())
                .fetch();
    }

    private BooleanExpression betweenOrNull(
            DateTimePath<LocalDateTime> path,
            LocalDateTime from,
            LocalDateTime to
    ) {
        BooleanExpression ge = (from == null) ? null : path.goe(from);
        BooleanExpression lt = (to == null) ? null : path.lt(to);
        if (ge == null) return lt;
        if (lt == null) return ge;
        return ge.and(lt);
    }

    @Override
    public List<ProjectSuccessCount> findProjectSuccessCounts() {

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
                        ProjectSuccessCount.class,
                        project.id,
                        project.name,
                        successSum.coalesce(0L),
                        failedSum.coalesce(0L)
                ))
                .from(project)
                .leftJoin(deployment)
                .on(
                        deployment.project.id.eq(project.id),
                        deployment.isDeleted.isFalse()
                )
                .where(project.isDeleted.isFalse())
                .groupBy(project.id, project.name)
                .fetch();
    }

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

        DateTimeExpression<LocalDateTime> monthTrunc =
                Expressions.dateTimeTemplate(LocalDateTime.class,
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
                        projectId != null ? project.id.eq(projectId) : null,
                        projectName != null ? project.name.eq(projectName) : null
                )
                .groupBy(monthTrunc)
                .orderBy(monthTrunc.asc())
                .fetch();

        Map<YearMonth, Double> map = new HashMap<>();
        for (Tuple t : rows) {
            LocalDateTime monthStart = t.get(0, LocalDateTime.class);
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
                Expressions.numberTemplate(Integer.class, "EXTRACT(MONTH FROM {0})",
                        buildRun.endedAt);

        NumberExpression<Long> successCase = new CaseBuilder()
                .when(deployment.isDeployed.isTrue()).then(1L).otherwise(0L);

        NumberExpression<Long> failedCase = new CaseBuilder()
                .when(deployment.isDeployed.isFalse()).then(1L).otherwise(0L);

        SubQueryExpression<LocalDateTime> maxEndedSub =
                JPAExpressions.select(buildRun2.endedAt.max())
                        .from(buildRun2)
                        .where(buildRun2.deployment.id.eq(deployment.id));

        BooleanExpression projectFilter =
                (projectId == null) ? null : deployment.project.id.eq(projectId);

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
                Expressions.numberTemplate(Integer.class, "EXTRACT(YEAR FROM {0})",
                        buildRun.endedAt);

        NumberExpression<Long> successCase = new CaseBuilder()
                .when(deployment.isDeployed.isTrue()).then(1L).otherwise(0L);

        NumberExpression<Long> failedCase = new CaseBuilder()
                .when(deployment.isDeployed.isFalse()).then(1L).otherwise(0L);

        SubQueryExpression<LocalDateTime> maxEndedSub =
                JPAExpressions.select(buildRun2.endedAt.max())
                        .from(buildRun2)
                        .where(buildRun2.deployment.id.eq(deployment.id));

        BooleanExpression projectFilter =
                (projectId == null) ? null : deployment.project.id.eq(projectId);

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
                        deployment.isDeleted.isFalse(),
                        buildRun.isDeleted.isFalse(),
                        projectFilter
                )
                .groupBy(yearExpr)
                .orderBy(yearExpr.asc())
                .fetch();
    }

    @Override
    public List<StatisticsRepositoryCustom.BanTypeRow> findBanTypeCounts(Long projectId) {

        // ban.type 이 enum이면: b.type.stringValue()
        // 문자열이면: b.type (StringPath) 그대로 사용 가능
        StringExpression typeExpr = ban.type.stringValue();

        BooleanExpression filter =
                (projectId == null) ? project.isDeleted.isFalse()
                        : project.id.eq(projectId).and(project.isDeleted.isFalse());

        return jpaQueryFactory.select(
                        Projections.constructor(
                                StatisticsRepositoryCustom.BanTypeRow.class,
                                typeExpr,                         // type
                                projectBan.id.count()             // count (project_ban 기준 집계)
                        )
                )
                .from(projectBan)
                .join(projectBan.ban, ban)
                .join(projectBan.project, project)
                .where(
                        projectBan.isDeleted.isFalse(),
                        ban.isDeleted.isFalse(),
                        filter
                )
                .groupBy(typeExpr)
                .orderBy(typeExpr.asc())
                .fetch();
    }

    @Override
    public List<IntraBuildRow> fetchIntraDeploymentBuildRuns(Long projectId) {
        // 조건
        BooleanExpression filter = buildRun.isDeleted.isFalse()
                .and(deployment.isDeleted.isFalse())
                .and(project.isDeleted.isFalse())
                .and(deployment.isDeployed.isTrue())        // 같은 deployment 안에서 "다음 성공"을 만들려면, 최종적으로 성공한 배포만 대상으로
                .and(buildRun.startedAt.isNotNull());
        if (projectId != null) {
            filter = filter.and(project.id.eq(projectId));
        }

        return jpaQueryFactory
                .select(Projections.constructor(
                        IntraBuildRow.class,
                        deployment.id,         // deploymentId
                        project.id,         // projectId
                        project.name,       // projectName
                        buildRun.startedAt, // startedAt
                        buildRun.isBuild    // isBuild (false=실패, true=성공)
                ))
                .from(buildRun)
                .join(buildRun.deployment, deployment)
                .join(deployment.project, project)
                .where(filter)
                .orderBy(deployment.id.asc(), buildRun.startedAt.asc())
                .fetch();
    }

    @Override
    public List<CrossDeploymentRow> fetchCrossDeploymentEvents(Long projectId) {
        // 각 deployment별 대표 시작시각: min(build_run.started_at)
        DateTimeExpression<LocalDateTime> minStart =
                Expressions.dateTimeTemplate(LocalDateTime.class, "min({0})", buildRun.startedAt);

        BooleanExpression base = buildRun.isDeleted.isFalse()
                .and(deployment.isDeleted.isFalse())
                .and(project.isDeleted.isFalse())
                .and(deployment.isDeployed.isNotNull())
                .and(buildRun.startedAt.isNotNull());

        if (projectId != null) {
            base = base.and(project.id.eq(projectId));
        }

        return jpaQueryFactory
                .select(Projections.constructor(
                        CrossDeploymentRow.class,
                        project.id,                 // projectId
                        project.name,               // projectName
                        deployment.pullRequest.id,     // pullRequestId
                        minStart,             // 대표 startedAt (min)
                        deployment.isDeployed          // 해당 deployment의 최종 결과 true/false
                ))
                .from(buildRun)
                .join(buildRun.deployment, deployment)
                .join(deployment.project, project)
                .where(base)
                .groupBy(project.id, project.name, deployment.pullRequest.id, deployment.isDeployed)
                .orderBy(project.id.asc(), deployment.pullRequest.id.asc(), minStart.asc())
                .fetch();
    }

}
