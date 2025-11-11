package sys.be4man.domains.deployment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentScheduler {

    private final TaskScheduler scheduler;
    private final DeploymentWebhookService webhookService;

    public void scheduleDeployment(String webhookUrl, LocalDateTime startTime) {
        Instant triggerTime = startTime.atZone(ZoneId.systemDefault()).toInstant();
        scheduler.schedule(() -> webhookService.triggerJenkins(webhookUrl), Date.from(triggerTime));
        log.info("✅ Deployment scheduled at {} for {}", startTime, webhookUrl);
    }
}
