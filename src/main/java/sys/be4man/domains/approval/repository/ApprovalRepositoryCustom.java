package sys.be4man.domains.approval.repository;

import java.util.List;
import sys.be4man.domains.deployment.model.entity.Deployment;

public interface ApprovalRepositoryCustom {

    /**
     * 복구현황 목록 조회 (ROLLBACK 타입 Approval의 Deployment 조회)
     * - approval.type = 'ROLLBACK'
     * - deployment와 조인
     * - 정렬: deployment.createdAt DESC
     * - 페이지네이션 지원
     *
     * @param offset   시작 위치
     * @param pageSize 페이지 크기
     * @return 복구현황 Deployment 목록
     */
    List<Deployment> findRollbackDeployments(int offset, int pageSize);

    /**
     * 복구현황 목록 총 개수 조회
     *
     * @return 총 개수
     */
    long countRollbackDeployments();
}

