package sys.be4man.domains.approval.service;

import java.util.List;
import sys.be4man.domains.approval.dto.request.ApprovalCreateRequest;
import sys.be4man.domains.approval.dto.request.ApprovalDecisionRequest;
import sys.be4man.domains.approval.dto.response.ApprovalDetailResponse;
import sys.be4man.domains.approval.dto.response.ApprovalSummaryResponse;
import sys.be4man.domains.approval.model.type.ApprovalStatus;

public interface ApprovalService {

    List<ApprovalSummaryResponse> getApprovals(Long accountId, ApprovalStatus status);

    ApprovalDetailResponse getApprovalDetail(Long approvalId);

    Long saveDraft(ApprovalCreateRequest request);

    void submit(Long approvalId);

    Long createAndSubmit(ApprovalCreateRequest request);

    void cancel(Long approvalId, ApprovalDecisionRequest request);

    void approve(Long approvalId, ApprovalDecisionRequest request);

    void reject(Long approvalId, ApprovalDecisionRequest request);
}
