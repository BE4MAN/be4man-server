package sys.be4man.history.dto;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 정보를 포함한 응답 DTO (선택)
 * - Spring의 Page를 커스텀 형식으로 변환할 때 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryPageResponseDto {

    /**
     * 실제 데이터 목록
     */
    private List<HistoryResponseDto> content;

    /**
     * 현재 페이지 번호 (0부터 시작)
     */
    private int currentPage;

    /**
     * 페이지 크기
     */
    private int pageSize;

    /**
     * 전체 페이지 수
     */
    private int totalPages;

    /**
     * 전체 요소 수
     */
    private long totalElements;

    /**
     * 첫 페이지 여부
     */
    private boolean first;

    /**
     * 마지막 페이지 여부
     */
    private boolean last;

    /**
     * Spring Page → 커스텀 DTO 변환
     */
    public static HistoryPageResponseDto from(Page<HistoryResponseDto> page) {
        return HistoryPageResponseDto.builder()
                .content(page.getContent())
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}