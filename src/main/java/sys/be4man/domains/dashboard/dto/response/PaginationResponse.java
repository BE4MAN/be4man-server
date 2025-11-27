// 작성자 : 이원석
package sys.be4man.domains.dashboard.dto.response;

import java.util.List;

/**
 * 페이지네이션 정보가 포함된 응답 DTO
 *
 * @param data       데이터 목록
 * @param pagination 페이지네이션 정보
 */
public record PaginationResponse<T>(
        List<T> data,
        PaginationInfo pagination
) {
    /**
     * 페이지네이션 정보
     *
     * @param total      전체 항목 수
     * @param page       현재 페이지 번호
     * @param pageSize   페이지당 항목 수
     * @param totalPages 전체 페이지 수
     */
    public record PaginationInfo(
            long total,
            int page,
            int pageSize,
            int totalPages
    ) {
    }
}

