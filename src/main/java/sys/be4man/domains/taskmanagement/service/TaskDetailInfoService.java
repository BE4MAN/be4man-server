// 작성자 : 허겸
package sys.be4man.domains.taskmanagement.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.approval.model.type.ApprovalLineType;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.taskmanagement.dto.PlanContentDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDetailInfoService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public PlanContentDto buildPlanContent(Deployment deployment, List<Approval> planApprovals, BuildRun buildRun) {
        // ✅ deployment가 null인 경우 처리 (PLAN이 없는 경우)
        if (deployment == null) {
            log.warn("⚠️ deployment가 null입니다. planApprovals로만 계획서 생성 시도");

            // Approval이 없으면 null 반환
            if (planApprovals.isEmpty()) {
                log.warn("⚠️ planApprovals도 없습니다. null 반환");
                return null;
            }

            // Approval 정보로만 계획서 생성
            Approval planApproval = planApprovals.get(0);
            Deployment approvalDeployment = planApproval.getDeployment();

            if (approvalDeployment == null) {
                log.warn("⚠️ Approval의 deployment도 null입니다. 기본값으로 생성");
                return buildPlanContentFromApprovalOnly(planApproval, buildRun);
            }

            // Approval의 deployment를 사용
            deployment = approvalDeployment;
        }

        log.debug("계획서 내용 생성 - deploymentId: {}, planApprovals: {}", deployment.getId(), planApprovals.size());

        String content = deployment.getContent();
        String planStatus = "승인대기";

        // ✅ ApprovalLine 우선 확인
        if (!planApprovals.isEmpty()) {
            Approval planApproval = planApprovals.get(0);
            content = planApproval.getContent();

            // ✅ 기안자 ID 가져오기
            Long drafterAccountId = planApproval.getAccount() != null ? planApproval.getAccount().getId() : null;

            log.debug("Approval 정보 - id: {}, lines: {}, 기안자 account_id: {}",
                    planApproval.getId(), planApproval.getApprovalLines().size(), drafterAccountId);

            // 반려 확인
            boolean isRejected = false;
            for (ApprovalLine line : planApproval.getApprovalLines()) {
                if (line.getType() != ApprovalLineType.CC) {
                    // ✅ 기안자 제외
                    Long lineAccountId = line.getAccount() != null ? line.getAccount().getId() : null;
                    if (lineAccountId != null && lineAccountId.equals(drafterAccountId)) {
                        log.debug("  ApprovalLine - 기안자 제외: {}", line.getAccount().getName());
                        continue;
                    }

                    Boolean isApproved = line.getIsApproved();
                    log.debug("  ApprovalLine - account: {}, isApproved: {}",
                            line.getAccount().getName(), isApproved);

                    if (isApproved != null && !isApproved) {
                        isRejected = true;
                        break;
                    }
                }
            }

            // 전체 승인 완료 확인 (기안자 제외한 실제 승인자만 확인)
            List<ApprovalLine> actualApprovers = planApproval.getApprovalLines().stream()
                    .filter(line -> line.getType() != ApprovalLineType.CC)
                    .filter(line -> {
                        // ✅ 기안자 제외
                        Long lineAccountId = line.getAccount() != null ? line.getAccount().getId() : null;
                        return !(lineAccountId != null && lineAccountId.equals(drafterAccountId));
                    })
                    .toList();

            log.debug("실제 승인자 수 (기안자 제외): {}", actualApprovers.size());

            // 각 승인자의 승인 상태 상세 로그
            for (ApprovalLine approver : actualApprovers) {
                log.debug("  실제 승인자 - name: {}, isApproved: {}",
                        approver.getAccount() != null ? approver.getAccount().getName() : "null",
                        approver.getIsApproved());
            }

            // 실제 승인자가 있고, 모두 승인했는지 확인
            boolean allApproved = !actualApprovers.isEmpty() && actualApprovers.stream()
                    .allMatch(line -> {
                        Boolean isApproved = line.getIsApproved();
                        boolean approved = isApproved != null && isApproved;
                        log.debug("    승인 확인 - account: {}, isApproved: {}, result: {}",
                                line.getAccount() != null ? line.getAccount().getName() : "null",
                                isApproved, approved);
                        return approved;
                    });

            log.debug("isRejected: {}, allApproved: {}, actualApprovers.isEmpty: {}",
                    isRejected, allApproved, actualApprovers.isEmpty());

            if (isRejected) {
                planStatus = "반려";
            } else if (allApproved) {
                planStatus = "승인완료";
            } else {
                planStatus = "승인대기";
            }
        } else {
            // ✅ Approval이 없으면 Deployment 상태 확인
            log.debug("planApprovals 없음 - Deployment.status로 판단: {}", deployment.getStatus());

            if (deployment.getStatus() == DeploymentStatus.APPROVED) {
                planStatus = "승인완료";
            } else if (deployment.getStatus() == DeploymentStatus.REJECTED) {
                planStatus = "반려";
            } else if (deployment.getStatus() == DeploymentStatus.PENDING) {
                planStatus = "승인대기";
            }
        }

        log.debug("최종 planStatus: {}", planStatus);

        // ✅ 배포 종료 시각 결정 로직 개선
        LocalDateTime actualEndedAt = null;

        // 1. BuildRun의 종료 시각 우선 사용 (가장 정확한 실제 종료 시각)
        if (buildRun != null && buildRun.getEndedAt() != null) {
            actualEndedAt = buildRun.getEndedAt();
            log.debug("배포 종료 시각: BuildRun.endedAt 사용 - {}", actualEndedAt);
        }
        // 2. Deployment가 COMPLETED 상태면 updatedAt 사용 (실제 완료 처리 시각)
        else if (deployment.getStatus() == DeploymentStatus.COMPLETED && deployment.getUpdatedAt() != null) {
            actualEndedAt = deployment.getUpdatedAt();
            log.debug("배포 종료 시각: Deployment.updatedAt 사용 (COMPLETED 상태) - {}", actualEndedAt);
        }
        // 3. 계획된 종료 시각 사용
        else {
            actualEndedAt = deployment.getScheduledToEndedAt();
            log.debug("배포 종료 시각: scheduledToEndedAt 사용 (계획 시각) - {}", actualEndedAt);
        }

        return PlanContentDto.builder()
                .drafter(deployment.getIssuer() != null ? deployment.getIssuer().getName() : null)
                .department(deployment.getIssuer() != null && deployment.getIssuer().getDepartment() != null ?
                        deployment.getIssuer().getDepartment().getKoreanName() : null)
                .createdAt(formatDateTime(deployment.getCreatedAt()))
                .serviceName(deployment.getProject() != null ? deployment.getProject().getName() : null)
                .taskTitle(deployment.getTitle())
                .scheduledAt(formatDateTime(deployment.getScheduledAt()))
                .scheduledToEndedAt(formatDateTime(actualEndedAt))
                .expectedDuration(deployment.getExpectedDuration())
                .version(deployment.getVersion())
                .content(content)
                .planStatus(planStatus)
                .build();
    }

    /**
     * Approval 정보만으로 계획서 생성 (deployment가 없는 경우)
     */
    private PlanContentDto buildPlanContentFromApprovalOnly(Approval planApproval, BuildRun buildRun) {
        log.debug("Approval만으로 계획서 생성 - approvalId: {}", planApproval.getId());

        String content = planApproval.getContent();
        String planStatus = "승인대기";

        // ✅ 기안자 ID 가져오기
        Long drafterAccountId = planApproval.getAccount() != null ? planApproval.getAccount().getId() : null;

        // 승인 상태 확인
        boolean isRejected = false;
        for (ApprovalLine line : planApproval.getApprovalLines()) {
            if (line.getType() != ApprovalLineType.CC) {
                // ✅ 기안자 제외
                Long lineAccountId = line.getAccount() != null ? line.getAccount().getId() : null;
                if (lineAccountId != null && lineAccountId.equals(drafterAccountId)) {
                    continue;
                }

                Boolean isApproved = line.getIsApproved();
                if (isApproved != null && !isApproved) {
                    isRejected = true;
                    break;
                }
            }
        }

        // 전체 승인 완료 확인 (기안자 제외한 실제 승인자만 확인)
        List<ApprovalLine> actualApprovers = planApproval.getApprovalLines().stream()
                .filter(line -> line.getType() != ApprovalLineType.CC)
                .filter(line -> {
                    // ✅ 기안자 제외
                    Long lineAccountId = line.getAccount() != null ? line.getAccount().getId() : null;
                    return !(lineAccountId != null && lineAccountId.equals(drafterAccountId));
                })
                .toList();

        // 실제 승인자가 있고, 모두 승인했는지 확인
        boolean allApproved = !actualApprovers.isEmpty() && actualApprovers.stream()
                .allMatch(line -> {
                    Boolean isApproved = line.getIsApproved();
                    return isApproved != null && isApproved;
                });

        if (isRejected) {
            planStatus = "반려";
        } else if (allApproved) {
            planStatus = "승인완료";
        }

        return PlanContentDto.builder()
                .drafter(planApproval.getAccount() != null ? planApproval.getAccount().getName() : null)
                .department(planApproval.getAccount() != null && planApproval.getAccount().getDepartment() != null ?
                        planApproval.getAccount().getDepartment().getKoreanName() : null)
                .createdAt(formatDateTime(planApproval.getCreatedAt()))
                .serviceName(planApproval.getService())
                .taskTitle(planApproval.getTitle())
                .content(content)
                .planStatus(planStatus)
                .build();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }
}