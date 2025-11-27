// 작성자 : 이원석
package sys.be4man.domains.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sys.be4man.domains.auth.dto.AccountPrincipal;
import sys.be4man.domains.ban.model.type.BanType;
import sys.be4man.domains.ban.model.type.RecurrenceType;
import sys.be4man.domains.ban.model.type.RecurrenceWeekOfMonth;
import sys.be4man.domains.ban.model.type.RecurrenceWeekday;
import sys.be4man.domains.schedule.dto.request.CreateBanRequest;
import sys.be4man.domains.schedule.dto.response.BanConflictCheckResponse;
import sys.be4man.domains.schedule.dto.response.BanResponse;
import sys.be4man.domains.schedule.dto.response.DeploymentScheduleResponse;
import sys.be4man.domains.schedule.dto.response.ScheduleMetadataResponse;
import sys.be4man.domains.schedule.service.ScheduleService;
import sys.be4man.global.dto.response.ErrorResponse;

/**
 * 스케줄 관리 API 컨트롤러
 */
@Tag(name = "Schedule", description = "스케줄 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 스케줄 관리 메타데이터 조회
     */
    @Operation(summary = "스케줄 관리 메타데이터 조회", description = "프로젝트 목록과 작업 금지 유형 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ScheduleMetadataResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/metadata")
    public ResponseEntity<ScheduleMetadataResponse> getScheduleMetadata() {
        log.info("스케줄 관리 메타데이터 조회 요청");
        return ResponseEntity.ok(scheduleService.getScheduleMetadata());
    }

    /**
     * 작업 금지 기간 생성
     */
    @Operation(summary = "작업 금지 기간 생성", description = "새로운 작업 금지 기간을 생성합니다. MANAGER, HEAD 권한만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = BanResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 부족함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/bans")
    public ResponseEntity<BanResponse> createBan(
            @Valid @RequestBody CreateBanRequest request,
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        log.info("작업 금지 기간 생성 요청 - accountId: {}, title: {}", principal.accountId(), request.title());
        return ResponseEntity.ok(scheduleService.createBan(request, principal.accountId()));
    }

    /**
     * 배포 작업 목록 조회
     */
    @Operation(summary = "배포 작업 목록 조회", description = "지정된 기간 내의 배포 작업 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = DeploymentScheduleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/deployments")
    public ResponseEntity<List<DeploymentScheduleResponse>> getDeploymentSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(scheduleService.getDeploymentSchedules(startDate, endDate));
    }

    /**
     * 작업 금지 기간 목록 조회
     */
    @Operation(summary = "작업 금지 기간 목록 조회", description = "조건에 맞는 작업 금지 기간 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = BanResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/bans")
    public ResponseEntity<List<BanResponse>> getBanSchedules(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BanType type,
            @RequestParam(required = false) List<Long> projectIds
    ) {
        return ResponseEntity.ok(scheduleService.getBanSchedules(query, startDate, endDate, type, projectIds));
    }

    /**
     * 작업 금지 기간 취소
     */
    @Operation(summary = "작업 금지 기간 취소", description = "작업 금지 기간을 취소합니다. MANAGER, HEAD 권한만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(responseCode = "400", description = "이미 취소된 작업 금지 기간",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한이 부족함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "작업 금지 기간을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/bans/{banId}")
    public ResponseEntity<Void> cancelBan(
            @PathVariable Long banId,
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        log.info("작업 금지 기간 취소 요청 - banId: {}, accountId: {}", banId, principal.accountId());
        scheduleService.cancelBan(banId, principal.accountId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Ban 등록 전 충돌 Deployment 조회
     */
    @Operation(summary = "Ban 등록 전 충돌 Deployment 조회", description = "입력 폼이 모두 채워진 상태에서 충돌하는 Deployment 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = BanConflictCheckResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/bans/conflicts")
    public ResponseEntity<BanConflictCheckResponse> checkBanConflicts(
            @RequestParam @NotEmpty List<Long> projectIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @Positive Integer durationMinutes,
            @RequestParam(required = false) RecurrenceType recurrenceType,
            @RequestParam(required = false) RecurrenceWeekday recurrenceWeekday,
            @RequestParam(required = false) RecurrenceWeekOfMonth recurrenceWeekOfMonth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recurrenceEndDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate queryStartDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate queryEndDate
    ) {
        log.info("Ban 충돌 체크 요청 - projectIds: {}, startDate: {}, startTime: {}, queryRange: {} ~ {}",
                projectIds, startDate, startTime, queryStartDate, queryEndDate);
        return ResponseEntity.ok(scheduleService.checkBanConflicts(
                projectIds, startDate, startTime, durationMinutes,
                recurrenceType, recurrenceWeekday, recurrenceWeekOfMonth, recurrenceEndDate,
                queryStartDate, queryEndDate
        ));
    }
}

