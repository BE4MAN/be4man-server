package sys.be4man.history.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sys.be4man.domains.deployment.model.type.DeployStatus;
import sys.be4man.history.dto.HistoryPageResponseDto;
import sys.be4man.history.dto.HistoryResponseDto;
import sys.be4man.history.dto.HistorySearchRequestDto;
import sys.be4man.history.service.HistoryService;

import java.time.LocalDate;

/**
 * History(배포 이력) API Controller
 * - 배포 이력 조회, 검색, 필터링 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:5173",           // 로컬 개발
        "https://be4man-client.vercel.app", // Vercel 배포
})
public class HistoryController {

    private final HistoryService historyService;

    /**
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 페이징된 배포 이력 목록
     */
    @GetMapping
    public ResponseEntity<Page<HistoryResponseDto>> getAllHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("전체 배포 이력 조회 요청 - page: {}, size: {}", page, size);

        Page<HistoryResponseDto> history = historyService.getAllHistory(page, size);

        log.info("전체 배포 이력 조회 완료 - 총 {}건, 현재 페이지: {}/{}",
                history.getTotalElements(), page + 1, history.getTotalPages());

        return ResponseEntity.ok(history);
    }

    /**
     * @param status 배포 상태 (선택)
     * @param projectId 프로젝트 ID (선택)
     * @param startDate 시작 날짜 (선택)
     * @param endDate 종료 날짜 (선택)
     * @param sortBy 정렬 기준 (선택: latest/oldest)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 필터링된 배포 이력 목록
     */
    @GetMapping("/filter")
    public ResponseEntity<Page<HistoryResponseDto>> getFilteredHistory(
            @RequestParam(required = false) DeployStatus status,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "latest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("필터링된 배포 이력 조회 요청 - status: {}, projectId: {}, startDate: {}, endDate: {}, page: {}, size: {}",
                status, projectId, startDate, endDate, page, size);

        // DTO 생성
        HistorySearchRequestDto searchDto = HistorySearchRequestDto.builder()
                .status(status)
                .projectId(projectId)
                .startDate(startDate)
                .endDate(endDate)
                .sortBy(sortBy)
                .build();

        Page<HistoryResponseDto> history = historyService.getFilteredHistory(searchDto, page, size);

        log.info("필터링된 배포 이력 조회 완료 - 총 {}건", history.getTotalElements());

        return ResponseEntity.ok(history);
    }

    /**
     *
     * @param status 배포 상태 (PENDING, APPROVED, SUCCESS, FAILURE 등)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 상태별 배포 이력 목록
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<HistoryResponseDto>> getHistoryByStatus(
            @PathVariable DeployStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("상태별 배포 이력 조회 요청 - status: {}, page: {}, size: {}", status, page, size);

        Page<HistoryResponseDto> history = historyService.getHistoryByStatus(status, page, size);

        log.info("{} 상태 배포 이력 조회 완료 - 총 {}건", status, history.getTotalElements());

        return ResponseEntity.ok(history);
    }

    /**
     *
     * @param projectId 프로젝트 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 프로젝트별 배포 이력 목록
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<Page<HistoryResponseDto>> getHistoryByProject(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("프로젝트별 배포 이력 조회 요청 - projectId: {}, page: {}, size: {}",
                projectId, page, size);

        Page<HistoryResponseDto> history = historyService.getHistoryByProject(projectId, page, size);

        log.info("프로젝트 {} 배포 이력 조회 완료 - 총 {}건", projectId, history.getTotalElements());

        return ResponseEntity.ok(history);
    }

    /**
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 날짜 범위별 배포 이력 목록
     */
    @GetMapping("/date-range")
    public ResponseEntity<Page<HistoryResponseDto>> getHistoryByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("날짜 범위별 배포 이력 조회 요청 - startDate: {}, endDate: {}, page: {}, size: {}",
                startDate, endDate, page, size);

        Page<HistoryResponseDto> history = historyService.getHistoryByDateRange(
                startDate, endDate, page, size);

        log.info("날짜 범위 배포 이력 조회 완료 - 총 {}건", history.getTotalElements());

        return ResponseEntity.ok(history);
    }

    /**
     *
     * @param prNumber PR 번호
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return PR 번호로 검색된 배포 이력 목록
     */
    @GetMapping("/pr/{prNumber}")
    public ResponseEntity<Page<HistoryResponseDto>> searchByPrNumber(
            @PathVariable Integer prNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("PR 번호 검색 요청 - prNumber: {}, page: {}, size: {}", prNumber, page, size);

        Page<HistoryResponseDto> history = historyService.searchByPrNumber(prNumber, page, size);

        log.info("PR #{} 검색 완료 - 총 {}건", prNumber, history.getTotalElements());

        return ResponseEntity.ok(history);
    }

    /**
     *
     * @param name 브랜치명 (부분 검색어)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 브랜치명으로 검색된 배포 이력 목록
     */
    @GetMapping("/branch")
    public ResponseEntity<Page<HistoryResponseDto>> searchByBranch(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("브랜치명 검색 요청 - name: {}, page: {}, size: {}", name, page, size);

        Page<HistoryResponseDto> history = historyService.searchByBranch(name, page, size);

        log.info("브랜치 '{}' 검색 완료 - 총 {}건", name, history.getTotalElements());

        return ResponseEntity.ok(history);
    }

    /**
     *
     * @param id 배포 ID
     * @return 배포 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<HistoryResponseDto> getDeploymentDetail(
            @PathVariable Long id
    ) {
        log.info("배포 상세 조회 요청 - id: {}", id);

        try {
            HistoryResponseDto history = historyService.getDeploymentDetail(id);
            log.info("배포 상세 조회 완료 - id: {}", id);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            log.warn("배포 상세 조회 실패 - id: {}, error: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     *
     * @param searchDto 검색 조건 (prNumber, branch, status, projectId 등)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검색된 배포 이력 목록
     */
    @GetMapping("/search")
    public ResponseEntity<Page<HistoryResponseDto>> search(
            @ModelAttribute HistorySearchRequestDto searchDto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("통합 검색 요청 - searchDto: {}, page: {}, size: {}", searchDto, page, size);

        Page<HistoryResponseDto> history = historyService.search(searchDto, page, size);

        log.info("통합 검색 완료 - 총 {}건", history.getTotalElements());

        return ResponseEntity.ok(history);
    }

    /**
     *
     * @param searchDto 검색 조건
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 커스텀 페이징 응답 (HistoryPageResponseDto)
     */
    @GetMapping("/custom")
    public ResponseEntity<HistoryPageResponseDto> getCustomPageResponse(
            @ModelAttribute HistorySearchRequestDto searchDto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("커스텀 페이징 응답 요청 - searchDto: {}, page: {}, size: {}",
                searchDto, page, size);

        HistoryPageResponseDto response = historyService.getFilteredHistoryWithCustomPage(
                searchDto, page, size);

        log.info("커스텀 페이징 응답 완료 - 총 {}건", response.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 예외 처리 핸들러 (선택)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e
    ) {
        log.error("잘못된 요청 - error: {}", e.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 에러 응답 DTO (선택)
     */
    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String message;
    }
}