package sys.be4man.domains.analysis.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class JenkinsProgressiveLogClient {

    private final RestTemplate restTemplate;

    @Value("${jenkins.url}")
    private String jenkinsUrl;

    @Value("${jenkins.username}")
    private String jenkinsUser;

    @Value("${jenkins.password}")
    private String jenkinsToken;

    public record ConsoleChunk(String text, int nextStart, boolean hasMore) {}

    public ConsoleChunk fetchChunk(String jobName, int buildNumber, int start) {
        HttpHeaders headers = new HttpHeaders();
        String auth = jenkinsUser + ":" + jenkinsToken;
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);

        String url = jenkinsUrl
                + "/job/" + jobName
                + "/" + buildNumber
                + "/logText/progressiveText?start=" + start;

        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        String body = resp.getBody() != null ? resp.getBody() : "";

        String textSize = resp.getHeaders().getFirst("X-Text-Size");
        String moreData = resp.getHeaders().getFirst("X-More-Data");

        int nextStart = (textSize != null)
                ? Integer.parseInt(textSize)
                : start + body.getBytes(StandardCharsets.UTF_8).length;

        boolean hasMore = "true".equalsIgnoreCase(moreData);

        return new ConsoleChunk(body, nextStart, hasMore);
    }
}
