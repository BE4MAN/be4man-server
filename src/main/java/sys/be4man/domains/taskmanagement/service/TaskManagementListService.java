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
 * 작업 관리 목록 서비스
 * - 작업 목록 조회 및 검색/필터링 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskManagementListService {

    private final TaskManagementRepository taskManagementRepository;

    /**
     * 특정 작업 기본 정보 조회
     *
     * @param taskId 작업 ID
     * @return 작업 기본 DTO
     */
    public TaskManagementResponseDto getTaskBasicInfo(Long taskId) {
        log.debug("작업 기본 정보 조회 - taskId: {}", taskId);

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