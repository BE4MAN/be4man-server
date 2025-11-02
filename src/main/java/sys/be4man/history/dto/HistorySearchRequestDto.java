package sys.be4man.history.dto;

import lombok.*;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.model.type.ProcessingStatus;
import sys.be4man.domains.history.model.type.ProcessingStatus;

import java.time.LocalDate;

/**
 * History 페이지 검색/필터 조건 DTO
 * - 프론트엔드에서 전달하는 검색 조건
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorySearchRequestDto {
    private DeploymentStatus status;
    private Long projectId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer prNumber;
    private String branch;
    private String sortBy;

    // 새로운 필터 조건들 (선택사항)
    private String drafter;                     // 기안자명으로 검색
    private String department;                  // 부서명으로 검색
    private String serviceName;                 // 서비스명으로 검색
    private ProcessingStatus processingStage;    // 처리 단계로 필터링
    private ProcessingStatus processingStatus;  // 처리 상태로 필터링
}