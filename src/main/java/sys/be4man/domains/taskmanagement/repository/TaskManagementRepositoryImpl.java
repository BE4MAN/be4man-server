package sys.be4man.domains.taskmanagement.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.common.model.type.ProcessingStage;
import sys.be4man.domains.common.model.type.ProcessingStatus;
import sys.be4man.domains.common.model.type.ReportStatus;
import sys.be4man.domains.taskmanagement.dto.TaskManagementSearchDto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static sys.be4man.domains.deployment.model.entity.QDeployment.deployment;
import static sys.be4man.domains.account.model.entity.QAccount.account;
import static sys.be4man.domains.project.model.entity.QProject.project;
import static sys.be4man.domains.pullrequest.model.entity.QPullRequest.pullRequest;

@Repository
@RequiredArgsConstructor
public class TaskManagementRepositoryImpl implements TaskManagementRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Deployment> findTasksBySearchConditions(
            TaskManagementSearchDto searchDto,
            Pageable pageable
    ) {
        // 동적 쿼리 조건 생성
        BooleanBuilder builder = new BooleanBuilder();

        // 1. 삭제되지 않은 데이터만 조회
        builder.and(deployment.isDeleted.isFalse());

        // 2. 검색어 조건 (작업번호, 기안자, 서비스명, 작업제목)
        if (searchDto.hasSearchQuery()) {
            String query = searchDto.getSearchQuery().toLowerCase();
            BooleanBuilder searchBuilder = new BooleanBuilder();

            // 작업번호로 검색 (숫자인 경우)
            try {
                Long id = Long.parseLong(query);
                searchBuilder.or(deployment.id.eq(id));
            } catch (NumberFormatException e) {
                // 숫자가 아니면 무시
            }

            // 기안자 이름으로 검색
            searchBuilder.or(deployment.issuer.name.containsIgnoreCase(query));

            // 서비스명으로 검색
            searchBuilder.or(deployment.project.name.containsIgnoreCase(query));

            // 작업 제목으로 검색
            searchBuilder.or(deployment.title.containsIgnoreCase(query));

            builder.and(searchBuilder);
        }

        // 3. 처리 단계 필터 (계획서/배포/결과보고)
        if (!searchDto.isStageAll()) {
            builder.and(buildStageCondition(searchDto.getStage()));
        }

        // 4. 처리 상태 필터 (승인대기/반려/진행중/취소/완료)
        if (!searchDto.isStatusAll()) {
            builder.and(buildStatusCondition(searchDto.getStatus()));
        }

        // 5. 결과 필터 (성공/실패)
        if (!searchDto.isResultAll()) {
            builder.and(buildResultCondition(searchDto.getResult()));
        }

        // 6. 날짜 범위 필터
        if (searchDto.hasDateRange()) {
            LocalDateTime startDateTime = searchDto.getStartDate().atStartOfDay();
            LocalDateTime endDateTime = searchDto.getEndDate().atTime(LocalTime.MAX);

            builder.and(deployment.updatedAt.between(startDateTime, endDateTime));
        }

        // 7. 정렬 조건
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(searchDto.getSortBy());

        // 전체 개수 조회 (count 쿼리는 join 없이 실행)
        Long totalCount = queryFactory
                .select(deployment.count())
                .from(deployment)
                .where(builder)
                .fetchOne();

        long total = (totalCount != null) ? totalCount : 0L;

        // 페이징 적용 및 결과 조회 (fetchJoin 사용)
        List<Deployment> content = queryFactory
                .selectFrom(deployment)
                .leftJoin(deployment.project, project).fetchJoin()
                .leftJoin(deployment.issuer, account).fetchJoin()
                .leftJoin(deployment.pullRequest, pullRequest).fetchJoin()
                .where(builder)
                .orderBy(orderSpecifier)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 처리 단계 조건 생성
     */
    private BooleanBuilder buildStageCondition(String stage) {
        BooleanBuilder builder = new BooleanBuilder();

        if ("계획서".equals(stage)) {
            // STAGED, PENDING, APPROVED, REJECTED, CANCELED
            builder.or(deployment.status.eq(DeploymentStatus.STAGED))
                   .or(deployment.status.eq(DeploymentStatus.PENDING))
                   .or(deployment.status.eq(DeploymentStatus.APPROVED))
                   .or(deployment.status.eq(DeploymentStatus.REJECTED))
                   .or(deployment.status.eq(DeploymentStatus.CANCELED));
        } else if ("배포".equals(stage)) {
            // DEPLOYMENT 또는 COMPLETED (ReportStatus 없음)
            builder.or(deployment.status.eq(DeploymentStatus.DEPLOYMENT))
                   .or(deployment.status.eq(DeploymentStatus.COMPLETED)
                       .and(deployment.reportStatus.isNull()));
        } else if ("결과보고".equals(stage)) {
            // COMPLETED (ReportStatus 있음)
            builder.and(deployment.status.eq(DeploymentStatus.COMPLETED))
                   .and(deployment.reportStatus.isNotNull());
        }

        return builder;
    }

    /**
     * 처리 상태 조건 생성
     */
    private BooleanBuilder buildStatusCondition(String status) {
        BooleanBuilder builder = new BooleanBuilder();

        if ("승인대기".equals(status)) {
            builder.or(deployment.status.eq(DeploymentStatus.PENDING))
                   .or(deployment.status.eq(DeploymentStatus.STAGED))
                   .or(deployment.reportStatus.eq(ReportStatus.PENDING));
        } else if ("반려".equals(status)) {
            builder.or(deployment.status.eq(DeploymentStatus.REJECTED))
                   .or(deployment.reportStatus.eq(ReportStatus.REJECTED));
        } else if ("진행중".equals(status)) {
            builder.or(deployment.status.eq(DeploymentStatus.DEPLOYMENT));
        } else if ("취소".equals(status)) {
            builder.or(deployment.status.eq(DeploymentStatus.CANCELED));
        } else if ("완료".equals(status)) {
            builder.or(deployment.status.eq(DeploymentStatus.COMPLETED))
                   .or(deployment.status.eq(DeploymentStatus.APPROVED))
                   .or(deployment.reportStatus.eq(ReportStatus.APPROVED));
        }

        return builder;
    }

    /**
     * 결과 조건 생성
     */
    private BooleanBuilder buildResultCondition(String result) {
        BooleanBuilder builder = new BooleanBuilder();

        if ("성공".equals(result)) {
            builder.and(deployment.status.eq(DeploymentStatus.COMPLETED))
                   .and(deployment.isDeployed.eq(true));
        } else if ("실패".equals(result)) {
            builder.and(deployment.status.eq(DeploymentStatus.COMPLETED))
                   .and(deployment.isDeployed.eq(false));
        }

        return builder;
    }

    /**
     * 정렬 조건 생성
     */
    private OrderSpecifier<?> getOrderSpecifier(String sortBy) {
        if ("오래된순".equals(sortBy)) {
            return deployment.createdAt.asc();
        }
        // 기본값: 최신순
        return deployment.createdAt.desc();
    }
}
