package sys.be4man.domains.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}

