package sys.be4man.domains.approval.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.approval.model.type.ApprovalType;

@Getter
@Builder
public class ApprovalSummaryResponse {

    private Long id;
    private String title;
    private String service;

    private ApprovalType type;
    private ApprovalStatus status;
    private Boolean isApproved;

    private Long drafterAccountId;
    private String drafterName;

    private Long nextApproverAccountId;
    private String nextApproverName;

    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime updatedAt;

    private LocalDateTime rejectedAt;
    private String rejectedBy;
    private String rejectedReason;

    private LocalDateTime canceledAt;
    private String canceledBy;
    private String canceledReason;

    private String approvedBy;
    private String approvedReason;
}
