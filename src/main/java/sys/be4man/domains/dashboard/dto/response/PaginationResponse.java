package sys.be4man.domains.dashboard.dto.response;

/**
 * 페이지네이션 정보 응답 DTO
 *
 * @param total      전체 항목 수
 * @param page       현재 페이지 번호 (1부터 시작)
 * @param pageSize   페이지당 항목 수
 * @param totalPages 전체 페이지 수
 */
public record PaginationResponse(
        long total,
        int page,
        int pageSize,
        int totalPages
) {
    /**
     * 전체 항목 수와 페이지 정보로부터 PaginationResponse 생성
     *
     * @param total    전체 항목 수
     * @param page     현재 페이지 번호
     * @param pageSize 페이지당 항목 수
     * @return PaginationResponse
     */
    public static PaginationResponse of(long total, int page, int pageSize) {
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PaginationResponse(total, page, pageSize, totalPages);
    }
}




