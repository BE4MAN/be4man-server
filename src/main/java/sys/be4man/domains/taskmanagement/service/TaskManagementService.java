package sys.be4man.domains.taskmanagement.service;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.analysis.repository.BuildRunRepository;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.approval.repository.ApprovalRepository;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.taskmanagement.dto.*;
import sys.be4man.domains.taskmanagement.repository.TaskManagementRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskManagementService {

    private final TaskManagementRepository taskManagementRepository;
    private final ApprovalRepository approvalRepository;
    private final BuildRunRepository buildRunRepository;
    private final TaskDetailTimelineService taskDetailTimelineService;
    private final TaskDetailReportService taskDetailReportService;
    private final TaskDetailInfoService taskDetailInfoService;
    private final TaskDetailApprovalService taskDetailApprovalService;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public Page<TaskManagementResponseDto> getTaskList(TaskManagementSearchDto searchDto, int page, int size) {
        log.debug("작업 관리 목록 조회 - searchDto: {}, page: {}, size: {}", searchDto, page, size);

        if (searchDto == null) {
            searchDto = TaskManagementSearchDto.builder().sortBy("최신순").build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Deployment> deployments = taskManagementRepository.findTasksBySearchConditions(searchDto, pageable);

        log.debug("조회된 Deployment 수: {}", deployments.getTotalElements());

        // Deployment를 DTO로 변환
        List<TaskManagementResponseDto> dtoList = deployments.getContent().stream()
                .map(deployment -> {
                    List<Approval> planApprovals;

                    // RETRY/ROLLBACK은 자체 Approval 조회
                    if (deployment.getStage() == DeploymentStage.RETRY) {
                        planApprovals = approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                                deployment.getId(), ApprovalType.RETRY);
                    } else if (deployment.getStage() == DeploymentStage.ROLLBACK) {
                        planApprovals = approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                                deployment.getId(), ApprovalType.ROLLBACK);
                    } else {
                        // 일반 PLAN Approval 조회
                        planApprovals = approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                                deployment.getId(), ApprovalType.PLAN);
                    }

                    // REPORT 단계면 REPORT Approval도 조회
                    List<Approval> reportApprovals = null;
                    if (deployment.getStage() == DeploymentStage.REPORT) {
                        reportApprovals = approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                                deployment.getId(), ApprovalType.REPORT);
                    }

                    return new TaskManagementResponseDto(deployment, planApprovals, reportApprovals);
                })
                .collect(Collectors.toList());

        // PageImpl로 Page 객체 생성
        return new PageImpl<>(dtoList, pageable, deployments.getTotalElements());
    }

    public TaskManagementResponseDto getTaskDetail(Long taskId) {
        log.debug("작업 상세 조회 - taskId: {}", taskId);

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

    public TaskDetailResponseDto getTaskDetailFull(Long deploymentId) {
        log.debug("작업 전체 상세 조회 - deploymentId: {}", deploymentId);

        Deployment deployment = taskManagementRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다. ID: " + deploymentId));

        if (deployment.getIsDeleted()) {
            throw new IllegalArgumentException("삭제된 작업입니다.");
        }

        log.debug("Deployment 조회 완료 - deploymentId: {}, stage: {}, status: {}",
                deployment.getId(), deployment.getStage(), deployment.getStatus());

        Deployment planDeployment = deployment;
        Deployment deploymentTask = null;
        Deployment reportDeployment = null;

        Long pullRequestId = deployment.getPullRequest() != null ? deployment.getPullRequest().getId() : null;

        // ✅ 같은 프로세스의 deployment 조회 (PLAN 기준으로 그룹화)
        DeploymentStage maxStage = deployment.getStage();  // 가장 진행된 단계
        List<Deployment> relatedDeployments;

        // ✅ RETRY/ROLLBACK은 독립적인 프로세스 (자기 자신만 사용)
        if (deployment.getStage() == DeploymentStage.RETRY || deployment.getStage() == DeploymentStage.ROLLBACK) {
            log.debug("RETRY/ROLLBACK 독립 프로세스 - deploymentId: {}, stage: {}",
                    deployment.getId(), deployment.getStage());

            relatedDeployments = List.of(deployment);
            planDeployment = null;  // RETRY/ROLLBACK은 별도의 PLAN이 없음
            deploymentTask = deployment;  // 자기 자신이 배포 작업
            reportDeployment = null;

        } else if (deployment.getStage() == DeploymentStage.REPORT && pullRequestId != null) {
            // ✅ REPORT는 바로 직전에 생성된 RETRY/ROLLBACK을 찾기
            Long projectId = deployment.getProject().getId();

            // REPORT 바로 직전의 RETRY/ROLLBACK 찾기
            List<Deployment> retryRollbackList = taskManagementRepository.findAll().stream()
                    .filter(d -> !d.getIsDeleted())
                    .filter(d -> d.getProject() != null && d.getProject().getId().equals(projectId))
                    .filter(d -> d.getPullRequest() != null && d.getPullRequest().getId().equals(pullRequestId))
                    .filter(d -> d.getStage() == DeploymentStage.RETRY || d.getStage() == DeploymentStage.ROLLBACK)
                    .filter(d -> d.getCreatedAt().isBefore(deployment.getCreatedAt()) ||
                                (d.getCreatedAt().equals(deployment.getCreatedAt()) && d.getId() < deployment.getId()))
                    .sorted((d1, d2) -> {
                        int timeCompare = d2.getCreatedAt().compareTo(d1.getCreatedAt());
                        if (timeCompare != 0) return timeCompare;
                        return d2.getId().compareTo(d1.getId());
                    })
                    .limit(1)
                    .collect(Collectors.toList());

            if (!retryRollbackList.isEmpty()) {
                // RETRY/ROLLBACK 프로세스의 REPORT
                Deployment retryRollback = retryRollbackList.get(0);
                log.debug("REPORT는 RETRY/ROLLBACK 프로세스에 속함 - reportId: {}, retryId: {}, retryCreatedAt: {}",
                        deployment.getId(), retryRollback.getId(), retryRollback.getCreatedAt());

                relatedDeployments = List.of(retryRollback, deployment);
                planDeployment = null;
                deploymentTask = retryRollback;
                reportDeployment = deployment;
            } else {
                // 일반 PLAN 프로세스의 REPORT - PLAN 찾기로 진행
                log.debug("REPORT는 일반 PLAN 프로세스에 속함 - reportId: {}", deployment.getId());

                // ✅ Step 1: 현재 배포가 속한 PLAN 찾기
                List<Deployment> relatedPlanList = taskManagementRepository.findRelatedPlanList(
                    projectId,
                    pullRequestId,
                    deployment.getCreatedAt(),
                    deployment.getId()
            );

            if (relatedPlanList.isEmpty()) {
                log.warn("⚠️ PLAN이 없는 배포 - deploymentId: {}, stage: {}, createdAt: {}",
                        deployment.getId(), deployment.getStage(), deployment.getCreatedAt());

                // PLAN이 없으면 현재 배포만 표시
                relatedDeployments = List.of(deployment);
                planDeployment = deployment.getStage() == DeploymentStage.PLAN ? deployment : null;
                deploymentTask = deployment.getStage() == DeploymentStage.DEPLOYMENT ||
                        deployment.getStage() == DeploymentStage.RETRY ||
                        deployment.getStage() == DeploymentStage.ROLLBACK ? deployment : null;
                reportDeployment = deployment.getStage() == DeploymentStage.REPORT ? deployment : null;

            } else {
                Deployment relatedPlan = relatedPlanList.get(0);

                log.debug("✅ 현재 배포가 속한 PLAN 찾음 - deploymentId: {}, planId: {}, planCreatedAt: {}",
                        deployment.getId(), relatedPlan.getId(), relatedPlan.getCreatedAt());

                // ✅ Step 2: 다음 PLAN 찾기 (다음 프로세스의 시작점)
                List<Deployment> nextPlanList = taskManagementRepository.findNextPlanList(
                        projectId,
                        pullRequestId,
                        relatedPlan.getCreatedAt(),
                        relatedPlan.getId()
                );

                LocalDateTime endTime = nextPlanList.isEmpty() ? null : nextPlanList.get(0).getCreatedAt();
                Long endId = nextPlanList.isEmpty() ? null : nextPlanList.get(0).getId();

                log.debug("다음 PLAN 조회 - nextPlanId: {}, nextPlanCreatedAt: {}",
                        endId,
                        endTime);

                // ✅ Step 3: 프로세스 범위 내 모든 배포 조회 (QueryDSL 사용)
                relatedDeployments = taskManagementRepository.findProcessDeploymentsQueryDsl(
                        projectId,
                        pullRequestId,
                        relatedPlan.getCreatedAt(),
                        relatedPlan.getId(),
                        endTime,
                        endId
                );

                log.debug("✅ 프로세스 범위 조회 완료 - PLAN: {}, 다음 PLAN: {}, 배포 개수: {}",
                        relatedPlan.getId(),
                        endId,
                        relatedDeployments.size());

                // 가장 진행된 단계 찾기
                for (Deployment d : relatedDeployments) {
                    if (getStageOrder(d.getStage()) > getStageOrder(maxStage)) {
                        maxStage = d.getStage();
                    }
                }

                log.debug("가장 진행된 단계: {}", maxStage);

                // 계획서 찾기 (프로세스 내에는 PLAN이 하나만 있음)
                planDeployment = relatedDeployments.stream()
                        .filter(d -> d.getStage() == DeploymentStage.PLAN)
                        .findFirst()
                        .orElse(deployment);

                // 배포 작업 찾기 (시간순 정렬되어 있으므로 첫 번째)
                deploymentTask = relatedDeployments.stream()
                        .filter(d -> d.getStage() == DeploymentStage.DEPLOYMENT ||
                                d.getStage() == DeploymentStage.RETRY ||
                                d.getStage() == DeploymentStage.ROLLBACK)
                        .findFirst()
                        .orElse(null);

                // 결과보고 찾기
                reportDeployment = relatedDeployments.stream()
                        .filter(d -> d.getStage() == DeploymentStage.REPORT)
                        .findFirst()
                        .orElse(null);

                log.debug("✅ 프로세스 기반 조회 완료 - planId: {}, deploymentId: {}, reportId: {}",
                        planDeployment.getId(),
                        deploymentTask != null ? deploymentTask.getId() : "없음",
                        reportDeployment != null ? reportDeployment.getId() : "없음");
                }
            }

        } else if (pullRequestId != null) {
            Long projectId = deployment.getProject().getId();

            // ✅ Step 1: 현재 배포가 속한 PLAN 찾기 (일반 DEPLOYMENT, PLAN)
            List<Deployment> relatedPlanList = taskManagementRepository.findRelatedPlanList(
                    projectId,
                    pullRequestId,
                    deployment.getCreatedAt(),
                    deployment.getId()
            );

            if (relatedPlanList.isEmpty()) {
                log.warn("⚠️ PLAN이 없는 배포 - deploymentId: {}, stage: {}, createdAt: {}",
                        deployment.getId(), deployment.getStage(), deployment.getCreatedAt());

                relatedDeployments = List.of(deployment);
                planDeployment = deployment.getStage() == DeploymentStage.PLAN ? deployment : null;
                deploymentTask = deployment.getStage() == DeploymentStage.DEPLOYMENT ? deployment : null;
                reportDeployment = null;

            } else {
                Deployment relatedPlan = relatedPlanList.get(0);

                log.debug("✅ 현재 배포가 속한 PLAN 찾음 - deploymentId: {}, planId: {}, planCreatedAt: {}",
                        deployment.getId(), relatedPlan.getId(), relatedPlan.getCreatedAt());

                // ✅ Step 2: 다음 PLAN 찾기
                List<Deployment> nextPlanList = taskManagementRepository.findNextPlanList(
                        projectId,
                        pullRequestId,
                        relatedPlan.getCreatedAt(),
                        relatedPlan.getId()
                );

                LocalDateTime endTime = nextPlanList.isEmpty() ? null : nextPlanList.get(0).getCreatedAt();
                Long endId = nextPlanList.isEmpty() ? null : nextPlanList.get(0).getId();

                log.debug("다음 PLAN 조회 - nextPlanId: {}, nextPlanCreatedAt: {}",
                        endId,
                        endTime);

                // ✅ Step 3: RETRY/ROLLBACK이 범위 내에 있는지 먼저 확인 (프로세스 분리 지점)
                LocalDateTime finalEndTime = endTime;
                Long finalEndId = endId;
                List<Deployment> retryRollbackInRange = taskManagementRepository.findAll().stream()
                        .filter(d -> !d.getIsDeleted())
                        .filter(d -> d.getProject() != null && d.getProject().getId().equals(projectId))
                        .filter(d -> d.getPullRequest() != null && d.getPullRequest().getId().equals(pullRequestId))
                        .filter(d -> d.getStage() == DeploymentStage.RETRY || d.getStage() == DeploymentStage.ROLLBACK)
                        .filter(d -> {
                            // PLAN 이후 생성된 것만
                            boolean afterPlan = d.getCreatedAt().isAfter(relatedPlan.getCreatedAt()) ||
                                    (d.getCreatedAt().equals(relatedPlan.getCreatedAt()) && d.getId() > relatedPlan.getId());
                            if (!afterPlan) return false;

                            // 다음 PLAN 이전 생성된 것만 (없으면 모두 포함)
                            if (finalEndTime != null) {
                                return d.getCreatedAt().isBefore(finalEndTime) ||
                                        (d.getCreatedAt().equals(finalEndTime) && d.getId() < finalEndId);
                            }
                            return true;
                        })
                        .sorted((d1, d2) -> {
                            int timeCompare = d1.getCreatedAt().compareTo(d2.getCreatedAt());
                            if (timeCompare != 0) return timeCompare;
                            return d1.getId().compareTo(d2.getId());
                        })
                        .limit(1)
                        .collect(Collectors.toList());

                // RETRY/ROLLBACK이 있으면 그것을 프로세스 종료 지점으로 사용
                if (!retryRollbackInRange.isEmpty()) {
                    Deployment firstRetry = retryRollbackInRange.get(0);
                    log.debug("⚠️ 범위 내 RETRY/ROLLBACK 발견 - id: {}, 프로세스를 여기서 종료합니다", firstRetry.getId());
                    endTime = firstRetry.getCreatedAt();
                    endId = firstRetry.getId();
                }

                // ✅ Step 4: 프로세스 범위 내 모든 배포 조회
                relatedDeployments = taskManagementRepository.findProcessDeploymentsQueryDsl(
                        projectId,
                        pullRequestId,
                        relatedPlan.getCreatedAt(),
                        relatedPlan.getId(),
                        endTime,
                        endId
                );

                log.debug("✅ 프로세스 범위 조회 완료 - PLAN: {}, 종료점: {}, 배포 개수: {}",
                        relatedPlan.getId(),
                        endId,
                        relatedDeployments.size());

                // ✅ 조회된 배포들 상세 로깅
                for (Deployment d : relatedDeployments) {
                    log.debug("  - 프로세스 내 배포: id={}, stage={}, createdAt={}",
                            d.getId(), d.getStage(), d.getCreatedAt());
                }

                // 가장 진행된 단계 찾기
                for (Deployment d : relatedDeployments) {
                    if (getStageOrder(d.getStage()) > getStageOrder(maxStage)) {
                        maxStage = d.getStage();
                    }
                }

                planDeployment = relatedDeployments.stream()
                        .filter(d -> d.getStage() == DeploymentStage.PLAN)
                        .findFirst()
                        .orElse(deployment);

                deploymentTask = relatedDeployments.stream()
                        .filter(d -> d.getStage() == DeploymentStage.DEPLOYMENT)
                        .findFirst()
                        .orElse(null);

                reportDeployment = relatedDeployments.stream()
                        .filter(d -> d.getStage() == DeploymentStage.REPORT)
                        .findFirst()
                        .orElse(null);
            }

        } else {
            // PR이 없으면 현재 배포만 표시
            log.debug("PR이 없는 배포 - deploymentId: {}", deployment.getId());
            relatedDeployments = List.of(deployment);
        }

        // ✅ Approval 조회
        List<Approval> planApprovals;

        // RETRY/ROLLBACK은 자체가 계획서를 포함하므로 해당 Deployment의 Approval 조회
        if (deployment.getStage() == DeploymentStage.RETRY) {
            planApprovals = approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                    deployment.getId(), ApprovalType.RETRY);
            log.debug("RETRY 계획서 조회 - deploymentId: {}, approvals: {}",
                    deployment.getId(), planApprovals.size());
        } else if (deployment.getStage() == DeploymentStage.ROLLBACK) {
            planApprovals = approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                    deployment.getId(), ApprovalType.ROLLBACK);
            log.debug("ROLLBACK 계획서 조회 - deploymentId: {}, approvals: {}",
                    deployment.getId(), planApprovals.size());
        } else if (deploymentTask != null && (deploymentTask.getStage() == DeploymentStage.RETRY ||
                                               deploymentTask.getStage() == DeploymentStage.ROLLBACK)) {
            // RETRY/ROLLBACK 프로세스의 REPORT인 경우
            ApprovalType approvalType = deploymentTask.getStage() == DeploymentStage.RETRY ?
                                       ApprovalType.RETRY : ApprovalType.ROLLBACK;
            planApprovals = approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                    deploymentTask.getId(), approvalType);
            log.debug("RETRY/ROLLBACK 프로세스의 REPORT - deploymentTaskId: {}, approvals: {}",
                    deploymentTask.getId(), planApprovals.size());
        } else if (planDeployment != null) {
            // 일반 프로세스는 PLAN Deployment의 Approval 조회
            planApprovals = approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                    planDeployment.getId(), ApprovalType.PLAN);
            log.debug("PLAN 계획서 조회 - planDeploymentId: {}, approvals: {}",
                    planDeployment.getId(), planApprovals.size());
        } else {
            // planDeployment가 null인 경우 (예외 상황)
            planApprovals = List.of();
            log.warn("⚠️ planDeployment가 null - deploymentId: {}, stage: {}",
                    deployment.getId(), deployment.getStage());
        }

        List<Approval> reportApprovals = reportDeployment != null ?
                approvalRepository.findByDeploymentIdAndTypeAndIsDeletedFalse(
                        reportDeployment.getId(), ApprovalType.REPORT) :
                List.of();

        log.debug("Approval 조회 완료 - planApprovals: {}, reportApprovals: {}",
                planApprovals.size(), reportApprovals.size());

        Long buildRunDeploymentId = deploymentTask != null ? deploymentTask.getId() : deployment.getId();
        Optional<BuildRun> buildRunOpt = buildRunRepository.findByDeploymentIdAndIsDeletedFalse(buildRunDeploymentId);

        log.debug("BuildRun 조회 - deploymentId: {}, found: {}", buildRunDeploymentId, buildRunOpt.isPresent());

        // ✅ 타임라인용 Deployment는 가장 진행된 것 사용
        Deployment timelineDeployment = reportDeployment != null ? reportDeployment :
                (deploymentTask != null ? deploymentTask : deployment);

        log.debug("타임라인용 Deployment - id: {}, stage: {}",
                timelineDeployment.getId(), timelineDeployment.getStage());

        List<TimelineStepDto> timeline = taskDetailTimelineService.buildTimeline(
                timelineDeployment, planApprovals, reportApprovals, buildRunOpt.orElse(null));

        JenkinsLogDto jenkinsLog = buildRunOpt.map(this::buildJenkinsLog).orElse(null);

        // RETRY/ROLLBACK은 자기 자신이 계획서, RETRY/ROLLBACK 프로세스의 REPORT는 deploymentTask 사용
        Deployment planContentDeployment;
        if (deployment.getStage() == DeploymentStage.RETRY || deployment.getStage() == DeploymentStage.ROLLBACK) {
            planContentDeployment = deployment;
        } else if (deploymentTask != null && (deploymentTask.getStage() == DeploymentStage.RETRY ||
                                               deploymentTask.getStage() == DeploymentStage.ROLLBACK)) {
            planContentDeployment = deploymentTask;
        } else {
            planContentDeployment = planDeployment;
        }
        PlanContentDto planContent = taskDetailInfoService.buildPlanContent(
                planContentDeployment, planApprovals, buildRunOpt.orElse(null));

        ApprovalInfoDto planApprovalInfo = taskDetailApprovalService.buildApprovalInfo(
                planApprovals, DeploymentStage.PLAN);

        ApprovalInfoDto reportApprovalInfo = reportDeployment != null ?
                taskDetailApprovalService.buildApprovalInfo(reportApprovals, DeploymentStage.REPORT) :
                null;

        Deployment reportTargetDeployment = reportDeployment != null ? reportDeployment : deployment;
        ReportContentDto reportContent = reportDeployment != null ?
                taskDetailReportService.buildReportContent(reportTargetDeployment, buildRunOpt.orElse(null), reportApprovals) :
                null;

        // ✅ initialTab 결정도 timelineDeployment 기준으로
        String currentStage;
        String currentStatus;
        String initialTab = "plan";

        if (timelineDeployment.getStage() == DeploymentStage.REPORT) {
            currentStage = "결과보고";
            if (timelineDeployment.getStatus() == DeploymentStatus.PENDING) {
                currentStatus = "승인대기";
                initialTab = "report";
            } else if (timelineDeployment.getStatus() == DeploymentStatus.COMPLETED) {
                currentStatus = "승인완료";
                initialTab = "report";
            } else if (timelineDeployment.getStatus() == DeploymentStatus.REJECTED) {
                currentStatus = "반려";
                initialTab = "report";
            } else {
                currentStatus = timelineDeployment.getStatus().getKoreanName();
                initialTab = "report";
            }
        } else if (timelineDeployment.getStage() == DeploymentStage.DEPLOYMENT ||
                timelineDeployment.getStage() == DeploymentStage.RETRY ||
                timelineDeployment.getStage() == DeploymentStage.ROLLBACK) {

            if (timelineDeployment.getStage() == DeploymentStage.ROLLBACK) {
                currentStage = "복구";
            } else if (timelineDeployment.getStage() == DeploymentStage.RETRY) {
                currentStage = "재배포";
            } else {
                currentStage = "배포";
            }

            if (timelineDeployment.getStatus() == DeploymentStatus.IN_PROGRESS) {
                currentStatus = "진행중";
                initialTab = "jenkins";
            } else if (timelineDeployment.getStatus() == DeploymentStatus.COMPLETED) {
                currentStatus = timelineDeployment.getIsDeployed() ? "성공" : "실패";
                initialTab = "jenkins";
            } else if (timelineDeployment.getStatus() == DeploymentStatus.REJECTED) {
                currentStatus = "반려";
                initialTab = "plan";
            } else if (timelineDeployment.getStatus() == DeploymentStatus.PENDING) {
                currentStatus = "대기";
                initialTab = "plan";
            } else {
                currentStatus = timelineDeployment.getStatus().getKoreanName();
                initialTab = "plan";
            }
        } else if (timelineDeployment.getStatus() == DeploymentStatus.APPROVED) {
            currentStage = "계획서";
            currentStatus = "승인완료";
            initialTab = "plan";
        } else if (timelineDeployment.getStatus() == DeploymentStatus.REJECTED) {
            currentStage = "계획서";
            currentStatus = "반려";
            initialTab = "plan";
        } else if (timelineDeployment.getStatus() == DeploymentStatus.PENDING) {
            currentStage = "계획서";
            currentStatus = "승인대기";
            initialTab = "plan";
        } else {
            currentStage = timelineDeployment.getStage().getKoreanName();
            currentStatus = timelineDeployment.getStatus().getKoreanName();
            initialTab = "plan";
        }

        // 가장 진행된 단계를 전달 (프론트엔드에서 탭 활성화 판단용)
        String maxStageKorean = getStageKorean(maxStage);

        return TaskDetailResponseDto.builder()
                .taskId(deployment.getId())
                .serviceName(deployment.getProject() != null ? deployment.getProject().getName() : null)
                .taskTitle(deployment.getTitle())
                .currentStage(currentStage)
                .currentStatus(currentStatus)
                .maxStage(maxStageKorean)  // ✅ 가장 진행된 단계 추가
                .initialTab(initialTab)
                .timeline(timeline)
                .jenkinsLog(jenkinsLog)
                .planContent(planContent)
                .planApproval(planApprovalInfo)
                .reportApproval(reportApprovalInfo)
                .reportContent(reportContent)
                .build();
    }

    // ✅ 단계 순서 반환 (비교용)
    private int getStageOrder(DeploymentStage stage) {
        switch (stage) {
            case PLAN: return 1;
            case DEPLOYMENT: return 2;
            case RETRY: return 2;
            case ROLLBACK: return 2;
            case REPORT: return 3;
            default: return 0;
        }
    }

    // ✅ 단계 한글명 반환
    private String getStageKorean(DeploymentStage stage) {
        switch (stage) {
            case PLAN: return "계획서";
            case DEPLOYMENT: return "배포";
            case RETRY: return "재배포";
            case ROLLBACK: return "복구";
            case REPORT: return "결과보고";
            default: return stage.getKoreanName();
        }
    }

    private JenkinsLogDto buildJenkinsLog(BuildRun buildRun) {
        String buildStatus = buildRun.getEndedAt() == null ? "IN_PROGRESS" : "SUCCESS";

        return JenkinsLogDto.builder()
                .jenkinsJobName(buildRun.getJenkinsJobName())
                .buildNumber(buildRun.getBuildNumber())
                .buildStatus(buildStatus)
                .startedAt(formatDateTime(buildRun.getStartedAt()))
                .endedAt(formatDateTime(buildRun.getEndedAt()))
                .duration(buildRun.getDuration())
                .durationFormatted(formatDuration(buildRun.getDuration()))
                .log(buildRun.getLog())
                .build();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    private String formatDuration(Long durationMs) {
        if (durationMs == null) return null;

        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        return minutes > 0 ? String.format("%d분 %d초", minutes, remainingSeconds) : String.format("%d초", remainingSeconds);
    }
}
