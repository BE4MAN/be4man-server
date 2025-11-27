// 작성자 : 조윤상
package sys.be4man.domains.analysis.repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class BuildSessionRegistry {

    public static class BuildSession {
        private final Long deploymentId;
        private final String jobName;
        private final int buildNumber;
        private final LocalDateTime startedAt;
        private volatile boolean completed;

        public BuildSession(Long deploymentId, String jobName, int buildNumber) {
            this.deploymentId = deploymentId;
            this.jobName = jobName;
            this.buildNumber = buildNumber;
            this.startedAt = LocalDateTime.now();
            this.completed = false;
        }

        public Long getDeploymentId() { return deploymentId; }
        public String getJobName() { return jobName; }
        public int getBuildNumber() { return buildNumber; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public boolean isCompleted() { return completed; }
        public void markCompleted() { this.completed = true; }
    }

    // 🔹 key를 deploymentId로 통일 (한 deployment 당 1개의 “현재 세션”만 관리)
    private final Map<Long, BuildSession> sessions = new ConcurrentHashMap<>();

    /** 빌드 시작 웹훅에서 호출 */
    public BuildSession createOrUpdate(Long deploymentId, int buildNumber, String jobName) {
        BuildSession session = new BuildSession(deploymentId, jobName, buildNumber);
        sessions.put(deploymentId, session);
        return session;
    }

    /** SSE 구독 시 deploymentId로 조회 */
    public BuildSession getByDeploymentId(Long deploymentId) {
        return sessions.get(deploymentId);
    }

    public void markCompleted(Long deploymentId) {
        BuildSession s = sessions.get(deploymentId);
        if (s != null) {
            s.markCompleted();
        }
    }

    public void remove(Long deploymentId) {
        sessions.remove(deploymentId);
    }
}