package sys.be4man.domains.approval.service;

import java.util.List;
import sys.be4man.domains.approval.dto.request.ApprovalCreateRequest;
import sys.be4man.domains.approval.dto.request.ApprovalDecisionRequest;
import sys.be4man.domains.approval.dto.request.ApprovalUpdateRequest;
import sys.be4man.domains.approval.dto.response.ApprovalDetailResponse;
import sys.be4man.domains.approval.dto.response.ApprovalSummaryResponse;
import sys.be4man.domains.approval.model.type.ApprovalStatus;

public interface ApprovalService {

    /** 특정 사용자의 결재 문서 목록 조회 (status 선택 필터) */
    List<ApprovalSummaryResponse> getApprovals(Long accountId, ApprovalStatus status);

    /** 결재 상세 조회 */
    ApprovalDetailResponse getApprovalDetail(Long approvalId);

    /** 임시 저장 (DRAFT) */
    Long saveDraft(ApprovalCreateRequest request);

    /**
     * 임시저장 문서 상신.
     * - 문서가 DRAFT인 경우: 새 PENDING 문서를 생성하고, 기존 DRAFT는 삭제한다.
     * - 그 외 상태인 경우: PENDING 상태로 보정하고 nextApprover를 세팅한다.
     */
    void submit(Long approvalId);

    /** 생성과 동시에 상신 (바로 PENDING 생성) */
    Long createAndSubmit(ApprovalCreateRequest request);

    /** 상신 취소 */
    void cancel(Long approvalId, ApprovalDecisionRequest request);

    /** 승인 */
    void approve(Long approvalId, ApprovalDecisionRequest request);

    /** 반려 */
    void reject(Long approvalId, ApprovalDecisionRequest request);

    /** 삭제 (DRAFT만 허용) */
    void delete(Long approvalId);

    /** 업데이트 (APPROVED/REJECTED/CANCELED는 불가) */
    void update(Long approvalId, ApprovalUpdateRequest request);
}
