// 작성자 : 조윤상
package sys.be4man.domains.statistics.dto.response;

import java.time.LocalDateTime;

public record MonthCountResponseDto(LocalDateTime month, Long cnt) {}
