package sys.be4man.domains.approval.dto.request;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.approval.model.type.ApprovalLineType;
import sys.be4man.domains.approval.model.type.ApprovalType;

@Getter
@NoArgsConstructor
public class ApprovalCreateRequest {

    private Long deploymentId;
    private Long drafterAccountId;
    private ApprovalType type;
    private String title;
    private String content;
    private String service;
    private Long projectId;
    private Long pullRequestId;

    private LocalDateTime scheduledAt;
    private LocalDateTime scheduledToEndedAt;

    private List<Long> relatedProjectIds;

    private List<ApprovalLineRequest> lines;

    @Getter
    @NoArgsConstructor
    public static class ApprovalLineRequest {
        private Long accountId;
        private ApprovalLineType type;
        private String comment;
    }
}
