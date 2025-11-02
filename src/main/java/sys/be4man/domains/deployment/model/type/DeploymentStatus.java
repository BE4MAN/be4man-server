package sys.be4man.domains.deployment.model.type;

import lombok.Getter;

/**
 * 배포 작업 상태 종류
 */
@Getter
public enum DeploymentStatus {
    PENDING("승인 대기중"),
    CANCELED("취소"),
    APPROVED("승인 완료 (실행 대기중)"),
    REJECTED("반려 완료"),
    SUCCESS("배포 성공"),
    FAILURE("배포 실패");
    ;

    private final String koreanName;

    DeploymentStatus(String koreanName) {
        this.koreanName = koreanName;
    }
}
