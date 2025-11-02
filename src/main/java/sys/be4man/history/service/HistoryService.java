package sys.be4man.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.history.dto.HistoryPageResponseDto;
import sys.be4man.history.dto.HistoryResponseDto;
import sys.be4man.history.dto.HistorySearchRequestDto;
import sys.be4man.history.repository.HistoryRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * History 페이지 서비스
 * - 배포 이력 조회 및 필터링
 * - Entity → DTO 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryService {

    private final HistoryRepository historyRepository;

    /**
     * 1. 전체 배포 이력 조회 (페이징)
     * - History 메인 화면
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 배포 이력 DTO
     */
    public Page<HistoryResponseDto> getAllHistory(int page, int size) {
        log.debug("전체 배포 이력 조회 - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Deployment> deployments = historyRepository.findAllHistory(pageable);

        log.debug("조회된 배포 이력 수: {}", deployments.getTotalElements());

        return deployments.map(HistoryResponseDto::new);
    }

    /**
     * 2. 필터링된 배포 이력 조회
     * - 상태, 프로젝트, 날짜 범위로 필터링
     *
     * @param searchDto 검색 조건
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 필터링된 배포 이력 DTO
     */
    public Page<HistoryResponseDto> getFilteredHistory(
            HistorySearchRequestDto searchDto,
            int page,
            int size
    ) {
        log.debug("필터링된 배포 이력 조회 - searchDto: {}, page: {}, size: {}",
                searchDto, page, size);

        // LocalDate → LocalDateTime 변환
        LocalDateTime startDateTime = searchDto.getStartDate() != null
                ? searchDto.getStartDate().atStartOfDay()  // 00:00:00
                : null;

        LocalDateTime endDateTime = searchDto.getEndDate() != null
                ? searchDto.getEndDate().atTime(LocalTime.MAX)  // 23:59:59.999999999
                : null;

        // 정렬 방향 결정
        Sort.Direction direction = "oldest".equals(searchDto.getSortBy())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        // Repository 호출
        Page<Deployment> deployments = historyRepository.findByFilters(
                searchDto.getStatus(),
                searchDto.getProjectId(),
                startDateTime,
                endDateTime,
                pageable
        );

        log.debug("필터링된 배포 이력 수: {}", deployments.getTotalElements());

        return deployments.map(HistoryResponseDto::new);
    }

    /**
     * 3. 상태별 배포 이력 조회
     *
     * @param status 배포 상태 (PENDING, SUCCESS, FAILURE 등)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 상태별 배포 이력 DTO
     */
    public Page<HistoryResponseDto> getHistoryByStatus(
            sys.be4man.domains.deployment.model.type.DeploymentStatus status,
            int page,
            int size
    ) {
        log.debug("상태별 배포 이력 조회 - status: {}, page: {}, size: {}", status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Deployment> deployments = historyRepository.findByStatus(status, pageable);

        log.debug("조회된 {} 상태 배포 수: {}", status, deployments.getTotalElements());

        return deployments.map(HistoryResponseDto::new);
    }

    /**
     * 4. 프로젝트별 배포 이력 조회
     *
     * @param projectId 프로젝트 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 프로젝트별 배포 이력 DTO
     */
    public Page<HistoryResponseDto> getHistoryByProject(
            Long projectId,
            int page,
            int size
    ) {
        log.debug("프로젝트별 배포 이력 조회 - projectId: {}, page: {}, size: {}",
                projectId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Deployment> deployments = historyRepository.findByProjectId(projectId, pageable);

        log.debug("프로젝트 {} 배포 수: {}", projectId, deployments.getTotalElements());

        return deployments.map(HistoryResponseDto::new);
    }

    /**
     * 5. 날짜 범위별 배포 이력 조회
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 날짜 범위별 배포 이력 DTO
     */
    public Page<HistoryResponseDto> getHistoryByDateRange(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            int page,
            int size
    ) {
        log.debug("날짜 범위별 배포 이력 조회 - startDate: {}, endDate: {}, page: {}, size: {}",
                startDate, endDate, page, size);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Deployment> deployments = historyRepository.findByDateRange(
                startDateTime, endDateTime, pageable);

        log.debug("조회된 배포 수: {}", deployments.getTotalElements());

        return deployments.map(HistoryResponseDto::new);
    }

    /**
     * 6. PR 번호로 배포 이력 검색
     *
     * @param prNumber PR 번호
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return PR 번호로 검색된 배포 이력 DTO
     */
    public Page<HistoryResponseDto> searchByPrNumber(
            Integer prNumber,
            int page,
            int size
    ) {
        log.debug("PR 번호로 배포 이력 검색 - prNumber: {}, page: {}, size: {}",
                prNumber, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Deployment> deployments = historyRepository.findByPrNumber(prNumber, pageable);

        log.debug("PR #{} 검색 결과 수: {}", prNumber, deployments.getTotalElements());

        return deployments.map(HistoryResponseDto::new);
    }

    /**
     * 7. 브랜치명으로 배포 이력 검색 (부분 일치)
     *
     * @param branch 브랜치명 (부분 검색어)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 브랜치명으로 검색된 배포 이력 DTO
     */
    public Page<HistoryResponseDto> searchByBranch(
            String branch,
            int page,
            int size
    ) {
        log.debug("브랜치명으로 배포 이력 검색 - branch: {}, page: {}, size: {}",
                branch, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Deployment> deployments = historyRepository.findByBranchContaining(branch, pageable);

        log.debug("브랜치 '{}' 검색 결과 수: {}", branch, deployments.getTotalElements());

        return deployments.map(HistoryResponseDto::new);
    }

    /**
     * 8. 특정 배포 상세 조회
     *
     * @param deploymentId 배포 ID
     * @return 배포 상세 DTO
     * @throws IllegalArgumentException 배포를 찾을 수 없는 경우
     */
    public HistoryResponseDto getDeploymentDetail(Long deploymentId) {
        log.debug("배포 상세 조회 - deploymentId: {}", deploymentId);

        Deployment deployment = historyRepository.findDetailById(deploymentId);

        if (deployment == null) {
            log.warn("배포를 찾을 수 없음 - deploymentId: {}", deploymentId);
            throw new IllegalArgumentException("배포를 찾을 수 없습니다. ID: " + deploymentId);
        }

        return new HistoryResponseDto(deployment);
    }



    /**
     * 10. 통합 검색 (선택)
     * - PR 번호 또는 브랜치명으로 검색
     *
     * @param searchDto 검색 조건
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검색된 배포 이력 DTO
     */
    public Page<HistoryResponseDto> search(
            HistorySearchRequestDto searchDto,
            int page,
            int size
    ) {
        log.debug("통합 검색 - searchDto: {}", searchDto);

        // PR 번호 검색
        if (searchDto.getPrNumber() != null) {
            return searchByPrNumber(searchDto.getPrNumber(), page, size);
        }

        // 브랜치명 검색
        if (searchDto.getBranch() != null && !searchDto.getBranch().trim().isEmpty()) {
            return searchByBranch(searchDto.getBranch(), page, size);
        }

        // 필터링 검색
        return getFilteredHistory(searchDto, page, size);
    }
}