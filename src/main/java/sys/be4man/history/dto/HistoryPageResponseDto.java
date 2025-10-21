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

    private List<HistoryResponseDto> content;
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private boolean first;
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