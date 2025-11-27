// 작성자 : 허겸
package sys.be4man.domains.taskmanagement.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.taskmanagement.dto.TaskManagementSearchDto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static sys.be4man.domains.deployment.model.entity.QDeployment.deployment;
import static sys.be4man.domains.account.model.entity.QAccount.account;
import static sys.be4man.domains.project.model.entity.QProject.project;
import static sys.be4man.domains.pullrequest.model.entity.QPullRequest.pullRequest;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskManagementRepositoryImpl implements TaskManagementRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Deployment> findTasksBySearchConditions(
            TaskManagementSearchDto searchDto,
            Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        // 1. 삭제되지 않은 데이터만 조회
        builder.and(deployment.isDeleted.isFalse());

        // ✅ 2. DEPLOYMENT의 PENDING/APPROVED 상태만 제외 (계획서 단계에서 이미 표시됨)
        // RETRY/ROLLBACK은 PENDING부터 표시 (자체가 계획서를 포함)
        builder.and(
                deployment.stage.eq(DeploymentStage.DEPLOYMENT)
                .and(deployment.status.in(
                        DeploymentStatus.PENDING,
                        DeploymentStatus.APPROVED
                ))
                .not()
        );

        // 3. 검색어 조건
        if (searchDto.hasSearchQuery()) {
            String query = searchDto.getSearchQuery().toLowerCase();
            BooleanBuilder searchBuilder = new BooleanBuilder();

            try {
                Long id = Long.parseLong(query);
                searchBuilder.or(deployment.id.eq(id));
            } catch (NumberFormatException e) {
                // 숫자가 아니면 무시
            }

            searchBuilder.or(deployment.issuer.name.containsIgnoreCase(query));
            searchBuilder.or(deployment.project.name.containsIgnoreCase(query));
            searchBuilder.or(deployment.title.containsIgnoreCase(query));

            builder.and(searchBuilder);
        }

        // 4. 처리 단계 필터
        if (!searchDto.isStageAll()) {
            builder.and(buildStageCondition(searchDto.getStage()));
        }

        // 5. 처리 상태 필터
        if (!searchDto.isStatusAll()) {
            builder.and(buildStatusCondition(searchDto.getStatus()));
        }

        // 6. 결과 필터
        if (!searchDto.isResultAll()) {
            builder.and(buildResultCondition(searchDto.getResult()));
        }

        // 7. 날짜 범위 필터
        if (searchDto.hasDateRange()) {
            LocalDateTime startDateTime = searchDto.getStartDate().atStartOfDay();
            LocalDateTime endDateTime = searchDto.getEndDate().atTime(LocalTime.MAX);
            builder.and(deployment.updatedAt.between(startDateTime, endDateTime));
        }

        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(searchDto.getSortBy());

        Long totalCount = queryFactory
                .select(deployment.count())
                .from(deployment)
                .where(builder)
                .fetchOne();

        long total = (totalCount != null) ? totalCount : 0L;

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
     * ✅ 수정: 처리 단계 조건 생성
     *
     * 작업 내역 표시 규칙:
     * 1. 계획서 단계:
     *    - PENDING(승인대기): 승인 대기 중
     *    - APPROVED(승인완료): 승인 완료
     *    - REJECTED(반려): 반려
     *
     * 2. 배포 단계:
     *    - IN_PROGRESS(배포중): scheduledAt 시간에 자동 시작
     *    - COMPLETED(종료): 배포 완료
     *    - CANCELED(취소): 배포 중 취소
     *
     * 3. 결과보고 단계:
     *    - PENDING(승인대기): 승인 대기 중
     *    - REJECTED(반려): 누구 한 명이라도 반려 시 모든 승인자의 내역에 표시
     *    - APPROVED(완료): 모든 승인 완료
     *
     * 4. 재배포 단계:
     *    - 재배포 작업 전체 (RETRY Stage)
     *
     * 5. 복구 단계:
     *    - 복구 작업 전체 (ROLLBACK Stage)
     */
    private BooleanBuilder buildStageCondition(String stage) {
        BooleanBuilder builder = new BooleanBuilder();

        if ("계획서".equals(stage)) {
            // ✅ 계획서는 PLAN 단계의 모든 상태 표시 (PENDING, APPROVED, REJECTED)
            builder.and(deployment.stage.eq(DeploymentStage.PLAN));

        } else if ("배포".equals(stage)) {
            // 배포는 DEPLOYMENT 단계만 (배포중, 종료, 취소 표시)
            builder.and(deployment.stage.eq(DeploymentStage.DEPLOYMENT))
                    .and(deployment.status.in(
                            DeploymentStatus.IN_PROGRESS,
                            DeploymentStatus.COMPLETED,
                            DeploymentStatus.CANCELED
                    ));

        } else if ("결과보고".equals(stage)) {
            // 결과보고는 승인대기, 반려, 완료 표시
            builder.and(deployment.stage.eq(DeploymentStage.REPORT))
                    .and(deployment.status.in(
                            DeploymentStatus.PENDING,
                            DeploymentStatus.REJECTED,
                            DeploymentStatus.APPROVED,
                            DeploymentStatus.COMPLETED
                    ));

        } else if ("재배포".equals(stage)) {
            // ✅ 재배포는 RETRY 단계 전체 표시
            builder.and(deployment.stage.eq(DeploymentStage.RETRY));

        } else if ("복구".equals(stage)) {
            // ✅ 복구는 ROLLBACK 단계 전체 표시
            builder.and(deployment.stage.eq(DeploymentStage.ROLLBACK));
        }

        return builder;
    }


    /**
     * 처리 상태 조건 생성
     * 상태 매핑:
     * - 승인대기: PENDING (계획서/결과보고 단계)
     * - 배포중: IN_PROGRESS (배포 단계)
     * - 취소: CANCELED (배포 단계)
     * - 종료: COMPLETED (배포 단계)
     * - 반려: REJECTED (결과보고 단계)
     * - 완료: APPROVED (결과보고 단계)
     */
    private BooleanBuilder buildStatusCondition(String status) {
        BooleanBuilder builder = new BooleanBuilder();

        if ("승인대기".equals(status) || "대기".equals(status)) {
            builder.or(deployment.status.eq(DeploymentStatus.PENDING));
        } else if ("배포중".equals(status) || "진행중".equals(status)) {
            builder.or(deployment.status.eq(DeploymentStatus.IN_PROGRESS));
        } else if ("취소".equals(status)) {
            builder.or(deployment.status.eq(DeploymentStatus.CANCELED));
        } else if ("종료".equals(status)) {
            builder.or(deployment.status.eq(DeploymentStatus.COMPLETED));
        } else if ("반려".equals(status)) {
            builder.or(deployment.status.eq(DeploymentStatus.REJECTED));
        } else if ("완료".equals(status) || "승인".equals(status)) {
            builder.or(deployment.status.eq(DeploymentStatus.APPROVED));
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

    @Override
    public List<Deployment> findProcessDeploymentsQueryDsl(
            Long projectId,
            Long prId,
            LocalDateTime startTime,
            Long startId,
            LocalDateTime endTime,
            Long endId
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(deployment.project.id.eq(projectId))
                .and(deployment.pullRequest.id.eq(prId))
                .and(deployment.createdAt.gt(startTime)
                        .or(deployment.createdAt.eq(startTime).and(deployment.id.goe(startId))))
                .and(deployment.isDeleted.isFalse());

        // endTime과 endId가 null이 아닐 때만 조건 추가
        if (endTime != null && endId != null) {
            builder.and(deployment.createdAt.lt(endTime)
                    .or(deployment.createdAt.eq(endTime).and(deployment.id.lt(endId))));
        }

        // 일반 프로세스에서는 RETRY/ROLLBACK 제외 (독립 프로세스이므로)
        builder.and(deployment.stage.ne(DeploymentStage.RETRY))
                .and(deployment.stage.ne(DeploymentStage.ROLLBACK));

        return queryFactory
                .selectFrom(deployment)
                .leftJoin(deployment.project, project).fetchJoin()
                .leftJoin(deployment.issuer, account).fetchJoin()
                .leftJoin(deployment.pullRequest, pullRequest).fetchJoin()
                .where(builder)
                .orderBy(deployment.createdAt.asc(), deployment.id.asc())
                .fetch();
    }
}