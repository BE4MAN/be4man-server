// 작성자 : 김민호
package sys.be4man.domains.approval.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubMergeService {

    private static final String MAIN = "main";
    private final GithubClient githubClient;

    public void mergePrBlocking(String owner, String repo, int prNumber, String title) {
        try {
            GithubClient.MergeResult res = githubClient
                    .mergePullRequest(owner, repo, prNumber, title)
                    .onErrorResume(e -> {
                        log.error("GitHub PR merge failed: {}/{}, PR #{} - {}", owner, repo, prNumber, e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (res != null && Boolean.TRUE.equals(res.merged())) {
                log.info("✅ PR merged: {}/{}, PR #{} sha={}", owner, repo, prNumber, res.sha());
            } else {
                log.warn("⚠️ PR merge response: {}", res != null ? res.message() : "null");
            }
        } catch (Exception e) {
            log.error("❌ PR merge exception", e);
        }
    }

    public void mergeBranchBlocking(String owner, String repo, String base, String head, String message) {
        try {
            GithubClient.MergeResult res = githubClient
                    .mergeBranch(owner, repo, base, head, message)
                    .onErrorResume(e -> {
                        log.error("GitHub branch merge failed: {}/{}, {} <- {} - {}", owner, repo, base, head, e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (res != null && Boolean.TRUE.equals(res.merged())) {
                log.info("✅ Branch merged: {}/{}, {} <- {}, sha={}", owner, repo, base, head, res.sha());
            } else {
                log.warn("⚠️ Branch merge response: {}", res != null ? res.message() : "null");
            }
        } catch (Exception e) {
            log.error("❌ Branch merge exception", e);
        }
    }

    public void mergeHeadIntoMainBlocking(String owner, String repo, String headBranch, String message) {
        String msg = (message == null || message.isBlank())
                ? "[BE4MAN] Auto-merge " + headBranch + " -> " + MAIN
                : message;
        try {
            GithubClient.MergeResult res = githubClient
                    .mergeBranch(owner, repo, MAIN, headBranch, msg)
                    .onErrorResume(e -> {
                        log.error("GitHub merge(main) failed: {}/{}, main <- {} - {}", owner, repo, headBranch, e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (res != null && Boolean.TRUE.equals(res.merged())) {
                log.info("✅ Merged into main: {}/{}, main <- {}, sha={}", owner, repo, headBranch, res.sha());
            } else {
                log.warn("⚠️ Merge(main) response: {}", res != null ? res.message() : "null");
            }
        } catch (Exception e) {
            log.error("❌ mergeHeadIntoMainBlocking exception", e);
        }
    }
}
