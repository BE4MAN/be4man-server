package sys.be4man.domains.taskmanagement.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sys.be4man.domains.taskmanagement.dto.TaskManagementResponseDto;
import sys.be4man.domains.taskmanagement.dto.TaskManagementSearchDto;
import sys.be4man.domains.taskmanagement.service.TaskManagementService;

/**
 * 작업 관리 페이지 API Controller
 * - 작업 목록 조회, 검색, 필터링 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:5173",           // 로컬 개발
        "https://be4man-client.vercel.app", // Vercel 배포
})
public class TaskManagementController {

    private final TaskManagementService taskManagementService;

    /**
     * 작업 관리 목록 조회 (검색 및 필터링)
     *
     * GET /api/tasks
     *
     * @param searchQuery 검색어 (작업번호, 기안자, 서비스명)
     * @param stage 처리 단계 (전체/계획서/배포/결과보고)
     * @param status 처리 상태 (전체/승인대기/반려/진행중/취소/완료)
     * @param result 결과 (전체/성공/실패)
     * @param startDate 시작일 (YYYY-MM-DD)
     * @param endDate 종료일 (YYYY-MM-DD)
     * @param sortBy 정렬 순서 (최신순/오래된순)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 9)
     * @return 페이징된 작업 목록
     */
    @GetMapping
    public ResponseEntity<Page<TaskManagementResponseDto>> getTaskList(
            @RequestParam(required = false) String searchQuery,
            @RequestParam(required = false, defaultValue = "전체") String stage,
            @RequestParam(required = false, defaultValue = "전체") String status,
            @RequestParam(required = false, defaultValue = "전체") String result,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate endDate,
            @RequestParam(required = false, defaultValue = "최신순") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size
    ) {
        log.info("작업 관리 목록 조회 요청 - searchQuery: {}, stage: {}, status: {}, result: {}, " +
                "startDate: {}, endDate: {}, sortBy: {}, page: {}, size: {}",
                searchQuery, stage, status, result, startDate, endDate, sortBy, page, size);

        // DTO 생성
        TaskManagementSearchDto searchDto = TaskManagementSearchDto.builder()
                .searchQuery(searchQuery)
                .stage(stage)
                .status(status)
                .result(result)
                .startDate(startDate)
                .endDate(endDate)
                .sortBy(sortBy)
                .build();

        // 서비스 호출
        Page<TaskManagementResponseDto> taskList = taskManagementService.getTaskList(searchDto, page, size);

        log.info("작업 관리 목록 조회 완료 - 총 {}건, 현재 페이지: {}/{}",
                taskList.getTotalElements(), page + 1, taskList.getTotalPages());

        return ResponseEntity.ok(taskList);
    }

    /**
     * 특정 작업 상세 조회
     *
     * GET /api/tasks/{taskId}
     *
     * @param taskId 작업 ID
     * @return 작업 상세 정보
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskManagementResponseDto> getTaskDetail(
            @PathVariable Long taskId
    ) {
        log.info("작업 상세 조회 요청 - taskId: {}", taskId);

        try {
            TaskManagementResponseDto task = taskManagementService.getTaskDetail(taskId);
            log.info("작업 상세 조회 완료 - taskId: {}", taskId);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            log.warn("작업 상세 조회 실패 - taskId: {}, error: {}", taskId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 예외 처리 핸들러
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e
    ) {
        log.error("잘못된 요청 - error: {}", e.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                400,
                e.getMessage()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 에러 응답 DTO
     */
    public record ErrorResponse(int status, String message) {
    }
}
