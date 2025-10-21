package sys.be4man.history.dto;

import lombok.*;
import sys.be4man.domains.deployment.model.type.DeployStatus;

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
    private DeployStatus status;
    private Long projectId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer prNumber;
    private String branch;
    private String sortBy;
}