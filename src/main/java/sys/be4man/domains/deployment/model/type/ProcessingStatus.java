package sys.be4man.domains.deployment.model.type;

import lombok.Getter;

/**
 * 처리 단계
 * - 배포 작업의 전체 단계를 구분
 */
@Getter
public enum ProcessingStatus {
    PLAN("계획서"),
    DEPLOYMENT("배포"),
    REPORT("레포트");

    private final String koreanName;

    ProcessingStatus(String koreanName) {
        this.koreanName = koreanName;
    }
}
