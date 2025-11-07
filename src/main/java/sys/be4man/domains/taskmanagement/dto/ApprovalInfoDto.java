package sys.be4man.domains.taskmanagement.dto;

import lombok.*;

import java.util.List;

/**
 * 승인 정보 DTO
 * - 계획서 승인 또는 결과보고 승인 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalInfoDto {

    private String approvalStage;           // 승인 단계 (계획서/결과보고)
    private Integer totalApprovers;         // 총 승인자 수
    private Long current_approver_account_id;   // 현재 차례 승인자 순서 (null이면 모두 완료)
    private List<ApproverDto> approvers;    // 승인자 목록
}