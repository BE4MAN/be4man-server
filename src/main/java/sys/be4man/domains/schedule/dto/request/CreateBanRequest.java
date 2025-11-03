package sys.be4man.domains.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import sys.be4man.domains.ban.model.type.BanType;

/**
 * 작업 금지 기간 생성 요청 DTO
 */
public record CreateBanRequest(
        @NotBlank(message = "제목은 필수입니다")
        String title,

        String description,

        @NotNull(message = "시작일은 필수입니다")
        LocalDate startDate,

        @NotNull(message = "시작시간은 필수입니다")
        LocalTime startTime,

        @NotNull(message = "종료일은 필수입니다")
        LocalDate endDate,

        @NotNull(message = "종료시간은 필수입니다")
        LocalTime endTime,

        @NotNull(message = "작업 금지 유형은 필수입니다")
        BanType type,

        @NotEmpty(message = "연관 프로젝트는 최소 1개 이상 필요합니다")
        List<Long> relatedProjectIds
) {
}

