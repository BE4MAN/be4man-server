// 작성자 : 조윤상
package sys.be4man.domains.analysis.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class JenkinsProgressiveLogClient {

    private final RestTemplate restTemplate;

    @Value("${jenkins.url}")
    private String jenkinsUrl;

    @Value("${jenkins.username}")
    private String jenkinsUsername;

    @Value("${jenkins.password}")
    private String jenkinsPassword;

    /** progressiveText 한 번 호출한 결과를 표현하는 DTO */
    public record LogChunk(String text, int nextStart, boolean hasMore) {}

    /**
     * !! 중요 !!
     * 이 메서드는 "딱 한 번" Jenkins progressiveText 엔드포인트를 호출해서
     * - 현재 start 오프셋 이후의 로그 조각(text)
     * - 다음 오프셋(nextStart)
     * - 추가 로그 존재 여부(hasMore)
     * 만 리턴한다.
     *
     * 절대 이 메서드 안에서 while (hasMore) 같은 루프 돌지 않는다.
     * 루프는 JenkinsConsoleStreamingService.startStreaming(...) 쪽에서만 돈다.
     */
    public LogChunk fetchChunk(String jobName, int buildNumber, int start) {
        try {
            // Basic Auth 헤더
            HttpHeaders headers = new HttpHeaders();
            String auth = jenkinsUsername + ":" + jenkinsPassword;
            String encodedAuth = Base64.getEncoder()
                    .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            // progressiveText URL
            // 예: {jenkins}/job/{jobName}/{buildNumber}/logText/progressiveText?start=0
            String url = jenkinsUrl
                    + "/job/" + jobName
                    + "/" + buildNumber
                    + "/logText/progressiveText?start=" + start;

            ResponseEntity<String> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            String body = resp.getBody() != null ? resp.getBody() : "";

            // 헤더에서 다음 포인터와 hasMore 플래그 읽기
            String textSizeHeader = resp.getHeaders().getFirst("X-Text-Size");
            String moreDataHeader = resp.getHeaders().getFirst("X-More-Data");

            int nextStart;
            if (textSizeHeader != null) {
                try {
                    nextStart = Integer.parseInt(textSizeHeader);
                } catch (NumberFormatException nfe) {
                    // 실패 시 fallback: 현재 start + body 바이트 길이
                    nextStart = start + body.getBytes(StandardCharsets.UTF_8).length;
                }
            } else {
                nextStart = start + body.getBytes(StandardCharsets.UTF_8).length;
            }

            boolean hasMore = "true".equalsIgnoreCase(moreDataHeader);

            log.debug(
                    "[JenkinsProgressive] job={}, build={}, start={}, nextStart={}, hasMore={}, len={}",
                    jobName, buildNumber, start, nextStart, hasMore, body.length()
            );

            return new LogChunk(body, nextStart, hasMore);
        } catch (Exception e) {
            log.error("[JenkinsProgressive] fetchChunk failed: job={}, build={}, start={}, err={}",
                    jobName, buildNumber, start, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
