package sys.be4man.domains.deployment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import sys.be4man.domains.approval.service.GithubMergeService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentScheduler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final TaskScheduler scheduler;
    private final DeploymentWebhookService webhookService;
    private final GithubMergeService githubMergeService;

    private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    public void schedule(Long deploymentId, String webhookUrl, LocalDateTime startTime) {
        cancel(deploymentId);
        Date at = toFutureDate(startTime);
        var future = scheduler.schedule(() -> webhookService.triggerJenkins(webhookUrl), at);
        futures.put(deploymentId, future);
        log.info("✅ Jenkins scheduled: depId={}, at={}({})", deploymentId, startTime, at);
    }

    public void scheduleMergePr(Long deploymentId, String owner, String repo, int prNumber, String title, LocalDateTime when) {
        cancel(deploymentId);
        String baseTitle = (title == null || title.isBlank()) ? ("PR #" + prNumber) : title;
        String taggedTitle = baseTitle + " [DEPLOYMENT_ID=" + deploymentId + "]";
        Date at = toFutureDate(when);
        var future = scheduler.schedule(() -> githubMergeService.mergePrBlocking(owner, repo, prNumber, taggedTitle), at);
        futures.put(deploymentId, future);
        log.info("✅ GitHub PR-merge scheduled: depId={}, {}/{}, PR #{}, at={}({}), title={}", deploymentId, owner, repo, prNumber, when, at, taggedTitle);
    }

    public void scheduleMergeBranch(Long deploymentId, String owner, String repo, String base, String head, String message, LocalDateTime when) {
        cancel(deploymentId);
        String baseMsg = (message == null || message.isBlank()) ? String.format("Merge %s into %s", head, base) : message;
        String taggedMsg = baseMsg + " [DEPLOYMENT_ID=" + deploymentId + "]";
        Date at = toFutureDate(when);
        var future = scheduler.schedule(() -> githubMergeService.mergeBranchBlocking(owner, repo, base, head, taggedMsg), at);
        futures.put(deploymentId, future);
        log.info("✅ GitHub branch-merge scheduled: depId={}, {}/{}, {} <- {}, at={}({}), msg={}", deploymentId, owner, repo, base, head, when, at, taggedMsg);
    }

    public void scheduleMergeIntoMain(Long deploymentId, String owner, String repo, String head, String message, LocalDateTime when) {
        cancel(deploymentId);
        String taggedMsg = ((message == null || message.isBlank()) ? "[BE4MAN] Auto-merge " + head + " -> main" : message)
                + " [DEPLOYMENT_ID=" + deploymentId + "]";
        Date at = toFutureDate(when);
        var future = scheduler.schedule(() -> {
            log.info("▶️ RUN merge job: depId={}, repo={}/{}, main <- {}, at={}", deploymentId, owner, repo, head, at);
            try {
                githubMergeService.mergeHeadIntoMainBlocking(owner, repo, head, taggedMsg);
            } catch (Exception e) {
                log.error("❌ merge job crashed: depId={}", deploymentId, e);
            }
        }, at);
        futures.put(deploymentId, future);
        log.info("✅ scheduled: depId={}, {}/{}, main <- {}, at={}({})", deploymentId, owner, repo, head, when, at);
    }

    public void cancel(Long deploymentId) {
        var f = futures.remove(deploymentId);
        if (f != null) {
            f.cancel(false);
            log.info("🛑 Canceled schedule: depId={}", deploymentId);
        }
    }

    private Date toFutureDate(LocalDateTime when) {
        Instant now = Instant.now();
        Instant target = (when == null)
                ? now.plusSeconds(5)
                : when.atZone(ZONE).toInstant();
        if (!target.isAfter(now.plusMillis(500))) {
            target = now.plusSeconds(5);
        }
        return Date.from(target);
    }
}
