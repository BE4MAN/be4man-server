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

    /**
     * 상태 필터
     * - PENDING, APPROVED, SUCCESS, FAILURE 등
     */
    private DeployStatus status;

    /**
     * 프로젝트 ID 필터
     */
    private Long projectId;

    /**
     * 시작 날짜
     */
    private LocalDate startDate;

    /**
     * 종료 날짜
     */
    private LocalDate endDate;

    /**
     * PR 번호 검색
     */
    private Integer prNumber;

    /**
     * 브랜치명 검색 (부분 일치)
     */
    private String branch;

    /**
     * 정렬 기준 (선택)
     * - "latest": 최신순 (기본값)
     * - "oldest": 오래된 순
     */
    private String sortBy;
}