package sys.be4man.domains.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 승인/반려 처리 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalProcessRequestDto {

    /**
     * 승인 여부 (true: 승인, false: 반려)
     */
    private Boolean isApproved;

    /**
     * 코멘트
     */
    private String comment;
}