// 작성자 : 김민호
package sys.be4man.domains.approval.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.approval.model.type.ApprovalLineType;

@Getter
@Builder
public class ApprovalDetailResponse {

    private Long id;
    private Long deploymentId;

    private Long drafterAccountId;
    private String drafterName;
    private String drafterDept;
    private String drafterRank;

    private String title;
    private String content;
    private String service;
    private ApprovalType type;
    private ApprovalStatus status;
    private Boolean isApproved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ApprovalLineDto> lines;

    @Getter
    @Builder
    public static class ApprovalLineDto {
        private Long id;
        private Long accountId;
        private String accountName;
        private String deptName;
        private String rank;
        private ApprovalLineType type;
        private String comment;
        private LocalDateTime approvedAt;
        private String statusLabel;
    }
}
