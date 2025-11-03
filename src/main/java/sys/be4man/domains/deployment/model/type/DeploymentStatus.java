package sys.be4man.domains.deployment.model.type;

import lombok.Getter;

/**
 * 배포 작업 상태 종류
 */
@Getter
public enum DeploymentStatus {

    PENDING("승인대기"),
    REJECTED("반려"),
    IN_PROGRESS("진행중"),
    CANCELED("취소"),
    COMPLETED("완료"),
    APPROVED("승인");

    private final String koreanName;

    DeploymentStatus(String koreanName) {
        this.koreanName = koreanName;
    }
}


