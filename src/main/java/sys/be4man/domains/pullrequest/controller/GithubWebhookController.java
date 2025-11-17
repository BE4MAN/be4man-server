package sys.be4man.domains.pullrequest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import sys.be4man.domains.pullrequest.dto.response.PrMergedBody;
import sys.be4man.domains.pullrequest.model.entity.PullRequest;
import sys.be4man.domains.pullrequest.repository.PullRequestRepository;

@Slf4j
@RestController
@RequestMapping("/webhooks/github")
@RequiredArgsConstructor
public class GithubWebhookController {

    private final PullRequestRepository pullRequestRepository;
    private final ObjectMapper om;

    @Value("${webhooks.github.secret}")
    private String webhookSecret;

    @PostMapping("/pr-merged")
    public ResponseEntity<?> onPrMerged(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String sigHeader,
            @RequestBody byte[] bodyBytes
    ) {
        try {
            boolean ok = verify(sigHeader, bodyBytes, webhookSecret);
            if (!ok) {
                String calc = "sha256=" + hmacHex(bodyBytes, normalizeSecret(webhookSecret));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header("X-Debug-Calc", calc)
                        .body("invalid signature");
            }

            PrMergedBody body = om.readValue(bodyBytes, PrMergedBody.class);

            Integer prNumber = body.getPrNumber();
            String branch    = body.getBranch();
            String repoUrl   = body.getRepositoryUrl();
            String email     = StringUtils.hasText(body.getGithubEmail()) ? body.getGithubEmail() : null;
            Long githubId    = body.getGithubId();

            if (prNumber == null || !StringUtils.hasText(repoUrl)) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }

            PullRequest pr = pullRequestRepository
                    .findByPrNumberAndRepositoryUrl(prNumber, repoUrl)
                    .orElseGet(() -> PullRequest.builder()
                            .prNumber(prNumber)
                            .repositoryUrl(repoUrl)
                            .build());

            pr.updateBranch(branch);
            pr.updateGithubEmail(email);
            if (githubId != null) {
                pr.updateGithubId(githubId);
            }
            pr.touchUpdatedAt(LocalDateTime.now());
            pullRequestRepository.save(pr);

            log.info("✅ Upsert pull_request: pr={}, repo={}, branch={}, email={}, githubId={}",
                    prNumber, repoUrl, branch, email, githubId);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Webhook error", e);
            return ResponseEntity.status(500).body("server error");
        }
    }

    private boolean verify(String header, byte[] bodyBytes, String secretRaw) {
        if (!StringUtils.hasText(header) || !header.startsWith("sha256=")) {
            log.warn("verify: missing/invalid header='{}'", header);
            return false;
        }
        String headerHex = normalizeHeaderSig(header);
        String secret    = normalizeSecret(secretRaw);
        if (!StringUtils.hasText(secret)) {
            log.warn("verify: empty secret");
            return false;
        }
        try {
            String calcHex = hmacHex(bodyBytes, secret);
            if (headerHex.length() != calcHex.length()) {
                log.warn("verify: length mismatch headerLen={}, calcLen={}", headerHex.length(), calcHex.length());
                return false;
            }
            boolean ok = constantTimeEquals(headerHex, calcHex);
            if (!ok) {
                log.warn("verify: mismatch headerSig={}, calcSig={}, bodyLen={}",
                        headerHex, calcHex, bodyBytes.length);
            }
            return ok;
        } catch (Exception e) {
            log.error("verify exception", e);
            return false;
        }
    }

    private String normalizeHeaderSig(String header) {
        String hex = header.substring("sha256=".length());
        return hex.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeSecret(String secretRaw) {
        return (secretRaw == null) ? "" : secretRaw.strip();
    }

    private String hmacHex(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(body);
        return HexFormat.of().formatHex(sig);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

    @PostMapping(value = "/_secret_check", produces = "text/plain")
    public ResponseEntity<String> secretCheck() throws Exception {
        byte[] body = "PING".getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok(hmacHex(body, webhookSecret));
    }

    @PostMapping("/_probe")
    public ResponseEntity<String> probe(@RequestBody byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String calc = HexFormat.of().formatHex(mac.doFinal(body));
        return ResponseEntity.ok(calc);
    }

    @PostMapping(value = "/_probe_full", produces = "application/json")
    public ResponseEntity<String> probeFull(@RequestBody byte[] body) throws Exception {
        byte[] secretBytes = webhookSecret.getBytes(StandardCharsets.UTF_8);
        int secretLen = secretBytes.length;
        int secretTail = secretLen > 0 ? (secretBytes[secretLen - 1] & 0xFF) : -1;

        int bodyLen = body.length;
        String bodyHead = toHeadHex(body, 64);

        String asIs = hmacHex(body, webhookSecret);
        byte[] plusLf = new byte[body.length + 1];
        System.arraycopy(body, 0, plusLf, 0, body.length);
        plusLf[body.length] = (byte) '\n';
        String withLf = hmacHex(plusLf, webhookSecret);
        int end = body.length;
        while (end > 0) {
            byte b = body[end - 1];
            if (b == '\n' || b == '\r' || b == ' ' || b == '\t') end--;
            else break;
        }
        byte[] trimmed = new byte[end];
        System.arraycopy(body, 0, trimmed, 0, end);
        String trimmedHex = hmacHex(trimmed, webhookSecret);

        String json = """
        {
          "secretLen": %d,
          "secretTailByte": %d,
          "bodyLen": %d,
          "bodyHeadHex": "%s",
          "hmac": {
            "asIs": "%s",
            "withTrailingLF": "%s",
            "trimmedRight": "%s"
          }
        }
        """.formatted(secretLen, secretTail, bodyLen, bodyHead, asIs, withLf, trimmedHex);
        return ResponseEntity.ok(json);
    }

    private static String toHeadHex(byte[] bytes, int n) {
        if (bytes == null || bytes.length == 0 || n <= 0) return "";
        int len = Math.min(n, bytes.length);
        return HexFormat.of().formatHex(bytes, 0, len);
    }
}
