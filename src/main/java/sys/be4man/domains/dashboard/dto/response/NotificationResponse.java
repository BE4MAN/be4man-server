// 작성자 : 이원석
package sys.be4man.domains.dashboard.dto.response;

import java.time.LocalDateTime;

/**
 * 알림 목록 응답 DTO
 *
 * @param id            Deployment ID (알림 고유 ID가 없으므로 deployment ID 사용)
 * @param kind          알림 종류 (취소, 반려)
 * @param reason        사유 (취소/반려 사유, 반려의 경우 ApprovalLine.comment)
 * @param serviceName   서비스 이름 (deployment.projectName)
 * @param deploymentId  Deployment ID
 * @param deploymentTitle 배포 제목
 * @param canceledAt    취소 시각 (kind가 '취소'일 때만 제공, deployment.updatedAt)
 * @param rejectedAt    반려 시각 (kind가 '반려'일 때만 제공, deployment.updatedAt 또는 approval.updatedAt)
 */
public record NotificationResponse(
        Long id,
        String kind,
        String reason,
        String serviceName,
        Long deploymentId,
        String deploymentTitle,
        LocalDateTime canceledAt,
        LocalDateTime rejectedAt
) {
}

