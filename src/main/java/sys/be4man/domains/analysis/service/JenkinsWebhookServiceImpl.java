// 작성자 : 조윤상
package sys.be4man.domains.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.analysis.dto.request.JenkinsBuildStartRequest;
import sys.be4man.domains.analysis.dto.response.JenkinsWebhooksResponseDto;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.analysis.repository.BuildSessionRegistry;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.repository.DeploymentRepository;
import sys.be4man.domains.deployment.exception.type.DeploymentExceptionType;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.global.exception.NotFoundException;

@Slf4j
@RequiredArgsConstructor
@Service
public class JenkinsWebhookServiceImpl implements WebhookService {

    private final DeploymentRepository deploymentRepository;
    private final BuildSessionRegistry buildSessionRegistry;

    @Override
    @Transactional
    public void setDeployResult(JenkinsWebhooksResponseDto jenkinsData, boolean isDeployed) {
        Long deploymentId = jenkinsData.deploymentId();
        // deployment id로 배포 작업 조회
        Deployment deployment = deploymentRepository.findByIdAndIsDeletedFalse(deploymentId).orElseThrow(() -> new NotFoundException(
                DeploymentExceptionType.DEPLOYMENT_NOT_FOUND)
        );

        deployment.updateIsDeployed(isDeployed);
        deployment.updateStage(DeploymentStage.DEPLOYMENT);
        deployment.updateStatus(DeploymentStatus.COMPLETED);

    }

    @Override
    @Transactional
    public void onBuildStart(JenkinsBuildStartRequest request) {
        Long deploymentId = request.deploymentId();
        int buildNumber   = request.buildNumber();
        String jobName    = request.jobName();

        log.info("[JENKINS][START] depId={}, job={}, build={}",
                deploymentId, jobName, buildNumber);

        buildSessionRegistry.createOrUpdate(deploymentId, buildNumber, jobName);
    }
}
