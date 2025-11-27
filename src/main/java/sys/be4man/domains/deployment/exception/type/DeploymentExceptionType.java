// 작성자 : 조윤상
package sys.be4man.domains.deployment.exception.type;

import lombok.RequiredArgsConstructor;
import sys.be4man.global.exception.type.ExceptionType;

/**
 * 배포 작업(Deployment) 도메인 예외 타입
 */
@RequiredArgsConstructor
public enum DeploymentExceptionType implements ExceptionType {

    DEPLOYMENT_NOT_FOUND("배포 작업을 찾을 수 없습니다.");

    private final String message;

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }
}

