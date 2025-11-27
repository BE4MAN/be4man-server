// 작성자 : 김민호
package sys.be4man.domains.pullrequest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sys.be4man.domains.pullrequest.dto.request.PullRequestCreateRequest;
import sys.be4man.domains.pullrequest.dto.request.PullRequestUpdateRequest;
import sys.be4man.domains.pullrequest.dto.response.PullRequestResponse;
import sys.be4man.domains.pullrequest.service.PullRequestService;

@Tag(name = "Pull Request", description = "Pull Request 관련 API")
@RestController
@RequestMapping("/api/prs")
@RequiredArgsConstructor
@Slf4j
public class PullRequestController {

    private final PullRequestService pullRequestService;

    @Operation(
            summary = "내 PR 목록 조회",
            description = "githubId 기준으로 자신이 생성한 Pull Request 목록만 조회합니다."
    )
    @GetMapping
    public ResponseEntity<List<PullRequestResponse>> getAllByGithubId(
            @RequestParam Long githubId
    ) {
        log.warn(String.valueOf(githubId));
        return ResponseEntity.ok(pullRequestService.getAllByGithubId(githubId));
    }

    @Operation(
            summary = "단일 PR 상세 조회",
            description = "특정 PR ID를 기준으로 Pull Request 상세 정보를 조회합니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<PullRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(pullRequestService.getById(id));
    }

    @Operation(
            summary = "신규 PR 등록",
            description = """
            새로운 Pull Request 정보를 수동으로 등록합니다.<br>
            일반적으로 GitHub Webhook이 자동으로 처리하지만, 필요 시 관리자나 시스템에서 직접 등록할 수도 있습니다.
            """
    )
    @PostMapping
    public ResponseEntity<PullRequestResponse> create(@RequestBody PullRequestCreateRequest request) {
        return ResponseEntity.ok(pullRequestService.create(request));
    }

    @Operation(
            summary = "PR 수정",
            description = "기존 Pull Request 정보를 수정합니다. 주로 브랜치명 또는 저장소 URL 등의 메타데이터를 변경할 때 사용됩니다."
    )
    @PutMapping("/{id}")
    public ResponseEntity<PullRequestResponse> update(
            @PathVariable Long id,
            @RequestBody PullRequestUpdateRequest request
    ) {
        return ResponseEntity.ok(pullRequestService.update(id, request));
    }

    @Operation(
            summary = "PR 삭제",
            description = "등록된 Pull Request 정보를 삭제합니다. 주로 테스트 데이터나 불필요한 기록 정리 시 사용됩니다."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pullRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "GitHub Webhook - 병합된 PR 등록",
            description = """
            GitHub Actions 또는 Webhook에서 호출되는 엔드포인트입니다.<br>
            `X-GitHub-Event: pull_request` 헤더가 포함된 요청만 처리하며,<br>
            병합(`merged = true`)된 PR 중 `base.ref`가 `develop` 브랜치인 경우에만 저장됩니다.<br><br>
            주요 처리 흐름:<br>
            1. GitHub에서 PR 병합 이벤트 발생<br>
            2. Webhook이 이 API(`/api/prs/github/merged`)를 호출<br>
            3. PR 번호, 저장소 URL, 브랜치 정보를 기반으로 PullRequest 엔티티 생성
            """
    )
    @PostMapping("/github/merged")
    public ResponseEntity<?> createFromGithub(
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestBody JsonNode payload
    ) {
        if (!"pull_request".equals(event)) {
            return ResponseEntity.ok("IGNORED_EVENT");
        }

        JsonNode prNode = payload.path("pull_request");
        boolean merged = prNode.path("merged").asBoolean(false);
        String baseRef = prNode.path("base").path("ref").asText("");

        Long githubId = prNode.path("user").path("id").asLong(0L);

        if (!merged || !baseRef.contains("develop")) {
            return ResponseEntity.ok("IGNORED_NOT_MERGED_OR_NOT_DEVELOP");
        }

        Integer prNumber = prNode.path("number").asInt();
        String repositoryUrl = payload.path("repository").path("html_url").asText(null);
        String branch = prNode.path("head").path("ref").asText(null); // feature/xxx

        PullRequestCreateRequest request = PullRequestCreateRequest.builder()
                .prNumber(prNumber)
                .repositoryUrl(repositoryUrl)
                .branch(branch)
                .githubId(githubId)
                .build();

        PullRequestResponse saved = pullRequestService.create(request);
        return ResponseEntity.ok(saved);
    }
}
