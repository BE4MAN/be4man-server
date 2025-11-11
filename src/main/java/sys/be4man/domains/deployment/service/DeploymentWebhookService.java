package sys.be4man.domains.deployment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class DeploymentWebhookService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void triggerJenkins(String webhookUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            restTemplate.postForEntity(webhookUrl, entity, String.class);
            log.info("🚀 Jenkins webhook triggered -> {}", webhookUrl);
        } catch (Exception e) {
            log.error("❌ Failed to trigger Jenkins webhook: {}", e.getMessage());
        }
    }
}
