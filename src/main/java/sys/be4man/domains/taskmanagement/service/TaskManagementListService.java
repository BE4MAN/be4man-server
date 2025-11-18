package sys.be4man.domains.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.taskmanagement.dto.TaskManagementResponseDto;
import sys.be4man.domains.taskmanagement.repository.TaskManagementRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskManagementListService {

    private final TaskManagementRepository taskManagementRepository;

    public TaskManagementResponseDto getTaskBasicInfo(Long taskId) {
        log.debug("작업 기본 정보 조회 - taskId: {}", taskId);

        Deployment deployment = taskManagementRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.warn("작업을 찾을 수 없음 - taskId: {}", taskId);
                    return new IllegalArgumentException("작업을 찾을 수 없습니다. ID: " + taskId);
                });

        if (deployment.getIsDeleted()) {
            log.warn("삭제된 작업 조회 시도 - taskId: {}", taskId);
            throw new IllegalArgumentException("삭제된 작업입니다. ID: " + taskId);
        }

        return new TaskManagementResponseDto(deployment);
    }
}