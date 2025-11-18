package sys.be4man.domains.approval.service;


import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class GithubClient {

    private final WebClient webClient;

    public GithubClient(
            @Value("${github.apiBaseUrl}") String apiBaseUrl,
            @Value("${github.token}") String token
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .build();
    }

    public Mono<MergeResult> mergePullRequest(String owner, String repo, int prNumber, String commitTitle) {
        record Body(String commit_title, String merge_method) {}
        Body body = new Body(commitTitle, "merge"); // squash/rebase 가능
        return webClient.put()
                .uri("/repos/{o}/{r}/pulls/{n}/merge", owner, repo, prNumber)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(MergeResult.class)
                .timeout(Duration.ofSeconds(20));
    }

    public Mono<MergeResult> mergeBranch(String owner, String repo, String base, String head, String commitMessage) {
        record Body(String base, String head, String commit_message) {}
        Body body = new Body(base, head, commitMessage);
        return webClient.post()
                .uri("/repos/{o}/{r}/merges", owner, repo)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(MergeResult.class)
                .timeout(Duration.ofSeconds(20));
    }

    public record MergeResult(Boolean merged, String sha, String message) {}
}