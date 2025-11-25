package sys.be4man.domains.taskmanagement.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * 작업 관리 페이지 검색/필터 조건 DTO
 * - 프론트엔드에서 전달하는 검색 및 필터 조건
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskManagementSearchDto {

    // 검색 조건
    private String searchQuery;             // 작업번호, 기안자, 서비스명 검색
    // 필터 조건
    private String stage;                   // 처리 단계 (전체/계획서/배포/결과보고/재배포/복구)
    private String status;                  // 처리 상태 (전체/승인대기/반려/진행중/취소/완료)
    private String result;                  // 결과 (전체/성공/실패)
    // 날짜 범위
    private LocalDate startDate;            // 시작일
    private LocalDate endDate;              // 종료일
    // 정렬
    private String sortBy;                  // 정렬 순서 (최신순/오래된순)

    /**
     * 필터 값이 "전체"인지 확인
     */
    public boolean isStageAll() {
        return stage == null || "전체".equals(stage);
    }

    public boolean isStatusAll() {
        return status == null || "전체".equals(status);
    }

    public boolean isResultAll() {
        return result == null || "전체".equals(result);
    }

    /**
     * 검색어가 있는지 확인
     */
    public boolean hasSearchQuery() {
        return searchQuery != null && !searchQuery.trim().isEmpty();
    }

    /**
     * 날짜 범위가 설정되어 있는지 확인
     */
    public boolean hasDateRange() {
        return startDate != null && endDate != null;
    }
}
