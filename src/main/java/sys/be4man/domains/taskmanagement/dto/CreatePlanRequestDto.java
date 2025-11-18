package sys.be4man.domains.taskmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 계획서 작성 요청 DTO
 * - 계획서를 작성하면 Deployment + Approval(PLAN) + ApprovalLine이 동시에 생성됨
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanRequestDto {

    // Deployment 정보
    private String title;                           // 배포 작업명
    private String content;                         // 배포 세부 내용 (계획서 내용)
    private LocalDateTime scheduledAt;              // 배포 예정 시간
    private LocalDateTime scheduledToEndedAt;       // 배포 예정 종료 시간
    private String expectedDuration;                // 예상 소요 시간
    private Long projectId;                         // 프로젝트 ID
    private Long pullRequestId;                     // Pull Request ID
    private Long issuerId;                          // 요청자 ID (기안자)

    // Approval 정보
    private String approvalTitle;                   // 승인 제목
    private String approvalContent;                 // 승인 내용
    private String service;                         // 서비스명

    // ApprovalLine 정보
    private List<Long> approverIds;                 // 승인자 ID 목록
}