package sys.be4man.domains.approval.dto.request;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.approval.model.type.ApprovalLineType;
import sys.be4man.domains.approval.model.type.ApprovalType;

@Getter
@NoArgsConstructor
public class ApprovalUpdateRequest {

    private String title;
    private String content;
    private String service;
    private ApprovalType type;

    private LocalDateTime scheduledAt;
    private LocalDateTime scheduledToEndedAt;

    private List<ApprovalLineRequest> lines;

    private List<Long> relatedProjectIds;

    @Getter
    @NoArgsConstructor
    public static class ApprovalLineRequest {
        private Long accountId;
        private ApprovalLineType type;
        private String comment;
    }
}
