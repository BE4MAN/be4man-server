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
    private final DeploymentService deploymentService;

    private final Map<Long, ScheduledFuture<?>> webhookFutures   = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> mergeFutures     = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> stageFlipFutures = new ConcurrentHashMap<>();

    public void scheduleWebhook(Long deploymentId, String webhookUrl, LocalDateTime when) {
        if (when == null) {
            log.info("⏭️ Skip webhook schedule: depId={}, reason=when is null", deploymentId);
            return;
        }
        cancelWebhook(deploymentId);
        Date at = toFutureDate(when);
        var future = scheduler.schedule(
                () -> {
                    log.info("▶️ RUN webhook: depId={}, at={}", deploymentId, at);
                    try {
                        webhookService.triggerJenkins(webhookUrl);
                    } catch (Exception e) {
                        log.error("❌ webhook job crashed: depId={}", deploymentId, e);
                    }
                },
                at
        );
        webhookFutures.put(deploymentId, future);
        log.info("✅ Jenkins scheduled: depId={}, at={}({})", deploymentId, when, at);
    }

    public void cancelWebhook(Long deploymentId) {
        cancelFuture(webhookFutures.remove(deploymentId), "webhook", deploymentId);
    }

    public void scheduleMergePr(Long deploymentId, String owner, String repo, int prNumber, String title, LocalDateTime when) {
        if (when == null) {
            log.info("⏭️ Skip PR-merge schedule: depId={}, reason=when is null", deploymentId);
            return;
        }
        cancelMerge(deploymentId);
        String baseTitle = (title == null || title.isBlank()) ? ("PR #" + prNumber) : title;
        String taggedTitle = baseTitle + " [DEPLOYMENT_ID=" + deploymentId + "]";
        Date at = toFutureDate(when);
        var future = scheduler.schedule(
                () -> {
                    log.info("▶️ RUN PR-merge: depId={}, {}/{}, PR #{}, at={}", deploymentId, owner, repo, prNumber, at);
                    try {
                        githubMergeService.mergePrBlocking(owner, repo, prNumber, taggedTitle);
                    } catch (Exception e) {
                        log.error("❌ PR-merge crashed: depId={}", deploymentId, e);
                    }
                },
                at
        );
        mergeFutures.put(deploymentId, future);
        log.info("✅ GitHub PR-merge scheduled: depId={}, {}/{}, PR #{}, at={}({}), title={}",
                deploymentId, owner, repo, prNumber, when, at, taggedTitle);
    }

    public void scheduleMergeBranch(Long deploymentId, String owner, String repo, String base, String head, String message, LocalDateTime when) {
        if (when == null) {
            log.info("⏭️ Skip branch-merge schedule: depId={}, reason=when is null", deploymentId);
            return;
        }
        cancelMerge(deploymentId);
        String baseMsg = (message == null || message.isBlank())
                ? String.format("Merge %s into %s", head, base)
                : message;
        String taggedMsg = baseMsg + " [DEPLOYMENT_ID=" + deploymentId + "]";
        Date at = toFutureDate(when);
        var future = scheduler.schedule(
                () -> {
                    log.info("▶️ RUN branch-merge: depId={}, {}/{}, {} <- {}, at={}", deploymentId, owner, repo, base, head, at);
                    try {
                        githubMergeService.mergeBranchBlocking(owner, repo, base, head, taggedMsg);
                    } catch (Exception e) {
                        log.error("❌ branch-merge crashed: depId={}", deploymentId, e);
                    }
                },
                at
        );
        mergeFutures.put(deploymentId, future);
        log.info("✅ GitHub branch-merge scheduled: depId={}, {}/{}, {} <- {}, at={}({}), msg={}",
                deploymentId, owner, repo, base, head, when, at, taggedMsg);
    }

    public void scheduleMergeIntoMain(Long deploymentId, String owner, String repo, String head, String message, LocalDateTime when) {
        if (when == null) {
            log.info("⏭️ Skip merge->main schedule: depId={}, reason=when is null", deploymentId);
            return;
        }
        cancelMerge(deploymentId);
        String taggedMsg = ((message == null || message.isBlank())
                ? "[BE4MAN] Auto-merge " + head + " -> main"
                : message) + " [DEPLOYMENT_ID=" + deploymentId + "]";
        Date at = toFutureDate(when);
        var future = scheduler.schedule(
                () -> {
                    log.info("▶️ RUN merge->main: depId={}, repo={}/{}, main <- {}, at={}", deploymentId, owner, repo, head, at);
                    try {
                        githubMergeService.mergeHeadIntoMainBlocking(owner, repo, head, taggedMsg);
                    } catch (Exception e) {
                        log.error("❌ merge->main crashed: depId={}", deploymentId, e);
                    }
                },
                at
        );
        mergeFutures.put(deploymentId, future);
        log.info("✅ scheduled merge->main: depId={}, {}/{}, main <- {}, at={}({})",
                deploymentId, owner, repo, head, when, at);
    }

    public void cancelMerge(Long deploymentId) {
        cancelFuture(mergeFutures.remove(deploymentId), "merge", deploymentId);
    }

    public void scheduleStageFlipToDeployment(Long deploymentId, LocalDateTime when) {
        if (when == null) {
            log.info("⏭️ Skip stage-flip schedule: depId={}, reason=when is null", deploymentId);
            return;
        }
        cancelStageFlip(deploymentId);
        Date at = toFutureDate(when);
        var future = scheduler.schedule(
                () -> {
                    log.info("▶️ RUN stage-flip: depId={}, at={}", deploymentId, at);
                    try {
                        deploymentService.flipStageToDeploymentIfDue(deploymentId);
                    } catch (Exception e) {
                        log.error("❌ stage-flip crashed: depId={}", deploymentId, e);
                    }
                },
                at
        );
        stageFlipFutures.put(deploymentId, future);
        log.info("✅ Stage flip scheduled: depId={}, at={}({})", deploymentId, when, at);
    }

    public void cancelStageFlip(Long deploymentId) {
        cancelFuture(stageFlipFutures.remove(deploymentId), "stage-flip", deploymentId);
    }

    public void cancelAll(Long deploymentId) {
        cancelWebhook(deploymentId);
        cancelMerge(deploymentId);
        cancelStageFlip(deploymentId);
    }

    private void cancelFuture(ScheduledFuture<?> f, String kind, Long depId) {
        if (f != null) {
            f.cancel(false);
            log.info("🛑 Canceled {} schedule: depId={}", kind, depId);
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
