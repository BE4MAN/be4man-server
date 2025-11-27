// 작성자 : 허겸
package sys.be4man.domains.taskmanagement.dto;

import lombok.*;

/**
 * 승인자 정보 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApproverDto {

    private Long approverId;                // 승인자 ID (account ID)
    private String approverName;            // 승인자 이름
    private String approverDepartment;      // 승인자 부서
    private Long current_approver_account_id;          // 승인 순서 (1, 2, 3...)
    private String approvalStatus;          // 승인 상태 (대기중/승인/반려)
    private String processedAt;             // 처리 시각 (YYYY.MM.DD HH:mm)
    private String comment;                 // 승인/반려 코멘트
    private Boolean isCurrentTurn;          // 현재 차례 여부
}