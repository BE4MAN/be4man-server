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
    private LocalDateTime createdAt;
}
