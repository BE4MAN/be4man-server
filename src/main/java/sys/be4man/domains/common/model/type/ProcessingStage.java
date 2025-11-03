package sys.be4man.domains.common.model.type;

import lombok.Getter;

/**
 * 처리 단계 (계획서 / 배포 / 결과보고)
 */
@Getter
public enum ProcessingStage {
    PLAN("계획서"),
    DEPLOYMENT("배포"),
    REPORT("결과보고");

    private final String koreanName;

    ProcessingStage(String koreanName) {
        this.koreanName = koreanName;
    }
}
