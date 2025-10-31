package sys.be4man.domains.analysis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.repository.DeploymentRepository;
import sys.be4man.domains.deployment.exception.type.DeploymentExceptionType;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.global.exception.NotFoundException;

@RequiredArgsConstructor
@Service
public class JenkinsWebhookServiceImpl implements WebhookService {

    // TODO 실제 deployment 도메인의 DeploymentRepository 만들어지면.. 거기에 추가.
    private final DeploymentRepository deploymentRepository;

    @Override
    @Transactional
    public void setDeployResult(long deploymentId, boolean isDeployed) {

        // deployment id로 배포 작업 조회
        Deployment deployment = deploymentRepository.findByIdAndIsDeletedFalse(deploymentId).orElseThrow(() -> new NotFoundException(
                DeploymentExceptionType.DEPLOYMENT_NOT_FOUND)
        );

        deployment.updateIsDeployed(isDeployed);
        deployment.updateStatus(DeploymentStatus.DEPLOYMENT);

    }
}
