package sys.be4man.domains.deployment.model.type;

import lombok.Getter;

/**
 * 배포 작업 상태 종류
 */
@Getter
public enum DeploymentStatus {
    STAGED("작업 계획서 신청 전"),
    PENDING("결제 대기"),
    APPROVED("승인 완료"),
    CANCELED("취소"),
    REJECTED("반려"),
    DEPLOYMENT("배포"),
    COMPLETED("배포 작업 완료");

    private final String koreanName;

    DeploymentStatus(String koreanName) {
        this.koreanName = koreanName;
    }
}


