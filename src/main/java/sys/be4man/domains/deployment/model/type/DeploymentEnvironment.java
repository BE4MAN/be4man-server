package sys.be4man.domains.deployment.model.type;

import lombok.Getter;

/**
 * 배포 작업 대상 환경
 */
@Getter
public enum DeploymentEnvironment {
    DEV("개발 환경"),
    PROD("운영 환경"),
    STAGING("스테이지 환경");

    private final String koreanName;

    DeploymentEnvironment(String koreanName) {
        this.koreanName = koreanName;
    }
}


