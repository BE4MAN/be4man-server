// 작성자 : 이원석
package sys.be4man.domains.deployment.model.type;

import lombok.Getter;

/**
 * 알림 종류
 */
@Getter
public enum NotificationType {
    APPROVED("배포 신청 승인"),
    REJECTED("배포 신청 반려"),
    SUCCESS("배포 성공"),
    FAILURE("배포 실패");

    private final String koreanName;

    NotificationType(String koreanName) {
        this.koreanName = koreanName;
    }
}


