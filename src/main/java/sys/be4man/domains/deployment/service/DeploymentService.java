package sys.be4man.domains.deployment.service;

import java.util.List;
import sys.be4man.domains.analysis.dto.response.DeploymentStageAndStatusResponseDto;
import sys.be4man.domains.deployment.dto.request.DeploymentCreateRequest;
import sys.be4man.domains.deployment.dto.response.DeploymentResponse;

public interface DeploymentService {

    DeploymentResponse createDeployment(DeploymentCreateRequest request);

    DeploymentResponse getDeployment(Long id);

    List<DeploymentResponse> getAllDeployments();

    void flipStageToDeploymentIfDue(Long deploymentId);

    DeploymentStageAndStatusResponseDto getBuildStageAndStatus(Long deploymentId);
}
