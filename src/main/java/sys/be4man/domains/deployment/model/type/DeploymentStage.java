package sys.be4man.domains.deployment.model.type;

import lombok.Getter;


@Getter
public enum DeploymentStage {
    PLAN("계획서"),
    DEPLOYMENT("배포"),
    REPORT("결과보고");

    private final String koreanName;

    DeploymentStage(String koreanName) {
        this.koreanName = koreanName;
    }
}