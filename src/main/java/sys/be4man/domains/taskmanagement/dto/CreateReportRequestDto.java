// 작성자 : 허겸
package sys.be4man.domains.taskmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 결과보고서 작성 요청 DTO
 * - 배포가 완료된 후 결과보고서를 작성하면 Approval(REPORT) + ApprovalLine이 생성됨
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequestDto {

    // 연결 정보
    private Long deploymentId;                      // Deployment ID (FK)
    private Long drafterAccountId;                  // 작성자 ID (기안자)

    // Approval 정보
    private String title;                           // 승인 제목
    private String content;                         // 결과 보고서 내용
    private String service;                         // 서비스명

    // ApprovalLine 정보
    private List<Long> approverIds;                 // 승인자 ID 목록
}