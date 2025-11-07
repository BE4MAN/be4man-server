package sys.be4man.domains.schedule.dto.response;

import java.util.List;
import sys.be4man.domains.ban.model.entity.Ban;

/**
 * 작업 금지 기간 응답 DTO
 */
public record BanResponse(
        String id,
        String title,
        String description,
        String startDate,
        String startTime,
        String endDate,
        String endTime,
        String type,
        List<String> relatedProjects
) {
    /**
     * Ban 엔티티와 연관 프로젝트 이름 목록으로부터 BanResponse 생성
     */
    public static BanResponse from(Ban ban, List<String> relatedProjects) {
        return new BanResponse(
                ban.getId().toString(),
                ban.getTitle(),
                ban.getDescription(),
                ban.getStartedAt().toLocalDate().toString(),
                ban.getStartedAt().toLocalTime().toString().substring(0, 5), // HH:mm
                ban.getEndedAt().toLocalDate().toString(),
                ban.getEndedAt().toLocalTime().toString().substring(0, 5), // HH:mm
                ban.getType().name(),
                relatedProjects
        );
    }
}

