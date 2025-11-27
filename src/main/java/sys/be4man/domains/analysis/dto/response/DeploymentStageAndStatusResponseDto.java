// 작성자 : 조윤상
package sys.be4man.domains.analysis.dto.response;

import lombok.Builder;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;

@Builder
public record DeploymentStageAndStatusResponseDto(Long deploymentId, DeploymentStage stage, DeploymentStatus status) {

}
