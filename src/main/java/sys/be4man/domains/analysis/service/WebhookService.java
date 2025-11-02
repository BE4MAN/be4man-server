package sys.be4man.domains.analysis.service;

// Jenkins 빌드 후 webhook을 다루는 서비스
public interface WebhookService {

    // Jenkins 빌드 후 deployment 테이블에서 빌드 성공 여부 설정하는 메서드
    public abstract void setDeployResult(long deploymentId, boolean isDeployed);

}
