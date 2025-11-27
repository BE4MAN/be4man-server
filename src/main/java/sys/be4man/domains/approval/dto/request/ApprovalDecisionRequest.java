// 작성자 : 김민호
package sys.be4man.domains.approval.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApprovalDecisionRequest {

    private Long approverAccountId;
    private String comment;
}
