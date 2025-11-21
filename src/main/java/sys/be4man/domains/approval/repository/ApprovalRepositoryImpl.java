package sys.be4man.domains.approval.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.taskmanagement.dto.TaskManagementSearchDto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static sys.be4man.domains.approval.model.entity.QApproval.approval;
import static sys.be4man.domains.account.model.entity.QAccount.account;
import static sys.be4man.domains.deployment.model.entity.QDeployment.deployment;
import static sys.be4man.domains.project.model.entity.QProject.project;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ApprovalRepositoryImpl implements ApprovalRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * ⚠️ 이 메서드는 더 이상 작업 목록 조회에 사용하지 않습니다.
     *
     * 작업 목록은 TaskManagementRepository에서 Deployment 테이블만 조회합니다.
     * Approval 정보는 작업 상세 페이지에서만 필요하며,
     * 그 경우 ApprovalRepository.findByDeploymentIdAndType() 메서드를 사용하세요.
     *
     * @deprecated 작업 목록 조회는 Deployment 테이블에서만 수행
     * @param searchDto 검색 조건 (사용 안 함)
     * @param pageable 페이징 정보 (사용 안 함)
     * @return 빈 페이지 (항상 빈 결과 반환)
     */
    @Override
    @Deprecated
    public Page<Approval> findApprovalsBySearchConditions(
            TaskManagementSearchDto searchDto,
            Pageable pageable
    ) {
        // ⚠️ 경고 로그 출력 - 이 메서드가 호출되면 안 됨
        log.warn("===============================================");
        log.warn("⚠️ WARNING: findApprovalsBySearchConditions() 호출됨!");
        log.warn("⚠️ 이 메서드는 사용하지 않아야 합니다.");
        log.warn("⚠️ 작업 목록은 Deployment 테이블에서만 조회해야 합니다.");
        log.warn("⚠️ 호출 스택을 확인하고 TaskManagementService.getTaskList()에서");
        log.warn("⚠️ 이 메서드를 호출하는 부분을 제거하세요.");
        log.warn("===============================================");

        // 스택 트레이스 출력 (디버깅용)
        if (log.isDebugEnabled()) {
            log.debug("호출 스택 트레이스:", new Exception("Stack trace"));
        }

        // ✅ 항상 빈 결과 반환
        // 이 메서드를 호출하는 곳이 있더라도 빈 결과를 반환하여 중복 방지
        List<Approval> emptyList = Collections.emptyList();
        return new PageImpl<>(emptyList, pageable, 0L);
    }

    /**
     * ✅ 상세 페이지용: 특정 Deployment의 Approval 조회
     *
     * @param deploymentId Deployment ID
     * @param type Approval 타입 (PLAN, REPORT)
     * @return 해당하는 Approval 목록
     */
    public List<Approval> findByDeploymentIdAndType(Long deploymentId, ApprovalType type) {
        log.debug("Deployment {} 의 {} 타입 Approval 조회", deploymentId, type);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(approval.deployment.id.eq(deploymentId));
        builder.and(approval.type.eq(type));
        builder.and(approval.deployment.isDeleted.isFalse());

        return queryFactory
                .selectFrom(approval)
                .leftJoin(approval.account, account).fetchJoin()
                .leftJoin(approval.deployment, deployment).fetchJoin()
                .where(builder)
                .orderBy(approval.id.asc())
                .fetch();
    }

    /**
     * ✅ 상세 페이지용: 특정 Deployment의 모든 Approval 조회
     *
     * @param deploymentId Deployment ID
     * @return 해당하는 모든 Approval 목록
     */
    public List<Approval> findByDeploymentId(Long deploymentId) {
        log.debug("Deployment {} 의 모든 Approval 조회", deploymentId);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(approval.deployment.id.eq(deploymentId));
        builder.and(approval.deployment.isDeleted.isFalse());

        return queryFactory
                .selectFrom(approval)
                .leftJoin(approval.account, account).fetchJoin()
                .leftJoin(approval.deployment, deployment).fetchJoin()
                .where(builder)
                .orderBy(approval.type.asc(), approval.id.asc())
                .fetch();
    }

    /**
     * ✅ 승인 대기 중인 Approval 조회 (대시보드용)
     *
     * @param accountId 승인자 계정 ID
     * @return 승인 대기 중인 Approval 목록
     */
    public List<Approval> findPendingApprovalsByApprover(Long accountId) {
        log.debug("계정 {} 의 승인 대기 Approval 조회", accountId);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(approval.nextApprover.id.eq(accountId));
        builder.and(approval.status.eq(ApprovalStatus.PENDING));
        builder.and(approval.deployment.isDeleted.isFalse());

        return queryFactory
                .selectFrom(approval)
                .leftJoin(approval.account, account).fetchJoin()
                .leftJoin(approval.deployment, deployment).fetchJoin()
                .leftJoin(approval.nextApprover).fetchJoin()
                .where(builder)
                .orderBy(approval.createdAt.desc())
                .fetch();
    }

    /**
     * 처리 상태 조건 생성 (내부용)
     */
    private BooleanBuilder buildStatusCondition(String status) {
        BooleanBuilder builder = new BooleanBuilder();

        if ("승인대기".equals(status)) {
            builder.or(approval.status.eq(ApprovalStatus.PENDING));
        } else if ("반려".equals(status)) {
            builder.or(approval.status.eq(ApprovalStatus.REJECTED));
        } else if ("완료".equals(status)) {
            builder.or(approval.status.eq(ApprovalStatus.APPROVED));
        }

        return builder;
    }

    /**
     * 정렬 조건 생성 (내부용)
     */
    private OrderSpecifier<?> getOrderSpecifier(String sortBy) {
        if ("오래된순".equals(sortBy)) {
            return approval.createdAt.asc();
        }
        // 기본값: 최신순
        return approval.createdAt.desc();
    }


    /**
     * 복구현황 목록 조회 (ROLLBACK 타입 Approval의 Deployment 조회)
     */
    @Override
    public List<Deployment> findRollbackDeployments(int offset, int pageSize) {
        log.debug("복구현황 목록 조회 - offset: {}, pageSize: {}", offset, pageSize);

        return queryFactory
                .selectFrom(deployment)
                .innerJoin(approval).on(approval.deployment.id.eq(deployment.id))
                .innerJoin(deployment.project, project).fetchJoin()
                .innerJoin(deployment.issuer, account).fetchJoin()
                .where(
                        approval.type.eq(ApprovalType.ROLLBACK)
                                .and(approval.isDeleted.eq(false))
                                .and(deployment.isDeleted.eq(false))
                )
                .orderBy(deployment.createdAt.desc())
                .offset(offset)
                .limit(pageSize)
                .distinct()
                .fetch();
    }

    /**
     * 복구현황 목록 총 개수 조회
     */
    @Override
    public long countRollbackDeployments() {
        log.debug("복구현황 목록 총 개수 조회");

        return queryFactory
                .select(deployment.id.countDistinct())
                .from(deployment)
                .innerJoin(approval).on(approval.deployment.id.eq(deployment.id))
                .where(
                        approval.type.eq(ApprovalType.ROLLBACK)
                                .and(approval.isDeleted.eq(false))
                                .and(deployment.isDeleted.eq(false))
                )
                .fetchOne();
    }
}