package sys.be4man.domains.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sys.be4man.domains.auth.dto.AccountPrincipal;
import sys.be4man.domains.dashboard.dto.response.PendingApprovalResponse;
import sys.be4man.domains.dashboard.service.DashboardService;
import sys.be4man.global.dto.response.ErrorResponse;

/**
 * 홈(Dashboard) 페이지 API 컨트롤러
 */
@Tag(name = "Dashboard", description = "홈(Dashboard) 페이지 API")
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 승인 대기 목록 조회
     */
    @Operation(summary = "승인 대기 목록 조회", description = "현재 사용자가 승인해야 하는 approval 리스트를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PendingApprovalResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/pending-approvals")
    public ResponseEntity<List<PendingApprovalResponse>> getPendingApprovals(
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        log.info("승인 대기 목록 조회 요청 - accountId: {}", principal.accountId());
        List<PendingApprovalResponse> response = dashboardService.getPendingApprovals(principal.accountId());
        return ResponseEntity.ok(response);
    }

    // TODO: Step 3에서 진행중인 업무 목록 조회 API 구현 예정
    // GET /api/dashboard/in-progress-tasks

    // TODO: Step 4에서 알림 목록 조회 API 구현 예정
    // GET /api/dashboard/notifications

    // TODO: Step 5에서 복구현황 목록 조회 API 구현 예정
    // GET /api/dashboard/recovery
}

