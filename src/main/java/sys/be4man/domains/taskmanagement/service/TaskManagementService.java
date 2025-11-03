package sys.be4man.domains.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.taskmanagement.dto.TaskManagementResponseDto;
import sys.be4man.domains.taskmanagement.dto.TaskManagementSearchDto;
import sys.be4man.domains.taskmanagement.repository.TaskManagementRepository;

/**
 * 작업 관리 페이지 서비스
 * - 작업 목록 조회 및 검색/필터링 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskManagementService {

    private final TaskManagementRepository taskManagementRepository;

    /**
     * 작업 관리 목록 조회 (검색 및 필터링 포함)
     *
     * @param searchDto 검색/필터 조건
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 작업 목록 DTO
     */
    public Page<TaskManagementResponseDto> getTaskList(
            TaskManagementSearchDto searchDto,
            int page,
            int size
    ) {
        log.debug("작업 관리 목록 조회 - searchDto: {}, page: {}, size: {}", searchDto, page, size);

        // 기본값 설정
        if (searchDto == null) {
            searchDto = TaskManagementSearchDto.builder()
                    .sortBy("최신순")
                    .build();
        }

        // 페이징 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // Repository에서 조회
        Page<Deployment> deployments = taskManagementRepository
                .findTasksBySearchConditions(searchDto, pageable);

        log.debug("조회된 작업 수: {}", deployments.getTotalElements());

        // Entity → DTO 변환
        return deployments.map(TaskManagementResponseDto::new);
    }

    /**
     * 특정 작업 상세 조회
     *
     * @param taskId 작업 ID
     * @return 작업 상세 DTO
     */
    public TaskManagementResponseDto getTaskDetail(Long taskId) {
        log.debug("작업 상세 조회 - taskId: {}", taskId);

        Deployment deployment = taskManagementRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.warn("작업을 찾을 수 없음 - taskId: {}", taskId);
                    return new IllegalArgumentException("작업을 찾을 수 없습니다. ID: " + taskId);
                });

        // 삭제된 작업인지 확인
        if (deployment.getIsDeleted()) {
            log.warn("삭제된 작업 조회 시도 - taskId: {}", taskId);
            throw new IllegalArgumentException("삭제된 작업입니다. ID: " + taskId);
        }

        return new TaskManagementResponseDto(deployment);
    }
}
