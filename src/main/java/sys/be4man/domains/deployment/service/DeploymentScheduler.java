package sys.be4man.domains.deployment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

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

    private final TaskScheduler scheduler;
    private final DeploymentWebhookService webhookService;
    private final GithubMergeService githubMergeService;

    private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    public void schedule(Long deploymentId, String webhookUrl, LocalDateTime startTime) {
        cancel(deploymentId);
        var future = scheduler.schedule(
                () -> webhookService.triggerJenkins(webhookUrl),
                Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant())
        );
        futures.put(deploymentId, future);
        log.info("✅ Jenkins scheduled: depId={}, at={}", deploymentId, startTime);
    }

    public void scheduleMergePr(Long deploymentId, String owner, String repo, int prNumber, String title, LocalDateTime when) {
        cancel(deploymentId);
        String baseTitle = (title == null || title.isBlank()) ? ("PR #" + prNumber) : title;
        String taggedTitle = baseTitle + " [DEPLOYMENT_ID=" + deploymentId + "]";
        var future = scheduler.schedule(
                () -> githubMergeService.mergePrBlocking(owner, repo, prNumber, taggedTitle),
                Date.from(when.atZone(ZoneId.systemDefault()).toInstant())
        );
        futures.put(deploymentId, future);
        log.info("✅ GitHub PR-merge scheduled: depId={}, {}/{}, PR #{}, at={}, title={}",
                deploymentId, owner, repo, prNumber, when, taggedTitle);
    }

    public void scheduleMergeBranch(Long deploymentId, String owner, String repo, String base, String head, String message, LocalDateTime when) {
        cancel(deploymentId);
        String baseMsg = (message == null || message.isBlank())
                ? String.format("Merge %s into %s", head, base)
                : message;
        String taggedMsg = baseMsg + " [DEPLOYMENT_ID=" + deploymentId + "]";
        var future = scheduler.schedule(
                () -> githubMergeService.mergeBranchBlocking(owner, repo, base, head, taggedMsg),
                Date.from(when.atZone(ZoneId.systemDefault()).toInstant())
        );
        futures.put(deploymentId, future);
        log.info("✅ GitHub branch-merge scheduled: depId={}, {}/{}, {} <- {}, at={}, msg={}",
                deploymentId, owner, repo, base, head, when, taggedMsg);
    }

    public void scheduleMergeIntoMain(
            Long deploymentId, String owner, String repo, String head, String message, LocalDateTime when
    ) {
        cancel(deploymentId);

        LocalDateTime now = LocalDateTime.now();
        if (when == null || !when.isAfter(now.plusSeconds(1))) {
            log.warn("⏱️ schedule time adjusted: depId={}, requested={}, adjusted=+5s", deploymentId, when);
            when = now.plusSeconds(5);
        }

        String taggedMsg = ((message == null || message.isBlank())
                ? "[BE4MAN] Auto-merge " + head + " -> main"
                : message) + " [DEPLOYMENT_ID=" + deploymentId + "]";

        LocalDateTime finalWhen = when;
        var future = scheduler.schedule(
                () -> {
                    log.info("▶️ RUN merge job: depId={}, repo={}/{}, main <- {}, at={}",
                            deploymentId, owner, repo, head, finalWhen);
                    try {
                        githubMergeService.mergeHeadIntoMainBlocking(owner, repo, head, taggedMsg);
                    } catch (Exception e) {
                        log.error("❌ merge job crashed: depId={}", deploymentId, e);
                    }
                },
                Date.from(when.atZone(ZoneId.systemDefault()).toInstant())
        );
        futures.put(deploymentId, future);
        log.info("✅ scheduled: depId={}, {}/{}, main <- {}, at={}", deploymentId, owner, repo, head, when);
    }

    public void cancel(Long deploymentId) {
        var f = futures.remove(deploymentId);
        if (f != null) {
            f.cancel(false);
            log.info("🛑 Canceled schedule: depId={}", deploymentId);
        }
    }
}
