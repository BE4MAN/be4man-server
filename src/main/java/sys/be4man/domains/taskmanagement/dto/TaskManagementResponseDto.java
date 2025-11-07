package sys.be4man.domains.taskmanagement.dto;

import lombok.*;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 작업 관리 페이지 응답 DTO
 * - 프론트엔드 LogManagement 컴포넌트에 맞춘 데이터 구조
 *
 * 워크플로우:
 * 1. 계획서 단계: 배포/재배포/복구 모두 "계획서"로 표시
 * 2. 배포 시작 (scheduledAt 도달): type에 따라 "배포", "재배포", "복구"로 표시
 * 3. 결과보고 단계: "결과보고"로 표시
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskManagementResponseDto {

    private Long id;                        // 작업 번호
    private String drafter;                 // 기안자
    private String department;              // 부서
    private String serviceName;             // 서비스명
    private String taskTitle;               // 작업 제목
    private String stage;                   // 처리 단계 (계획서/배포/재배포/복구/결과보고)
    private String status;                  // 처리 상태 (승인대기/대기/진행중/종료/완료/취소)
    private String completionTime;          // 완료 시각 (YYYY.MM.DD HH:mm)
    private String result;                  // 배포 결과 (성공/실패/null)

    /**
     * Deployment 엔티티로부터 DTO 생성
     */
    public TaskManagementResponseDto(Deployment deployment) {
        this.id = deployment.getId();
        this.drafter = getDrafterName(deployment);
        this.department = getDepartmentName(deployment);
        this.serviceName = getServiceName(deployment);
        this.taskTitle = deployment.getTitle();

        // 처리 단계 결정 (stage + type 조합)
        this.stage = determineDisplayStage(deployment);

        // 처리 상태 결정
        this.status = determineDisplayStatus(deployment);

        // 완료 시각 포맷팅
        this.completionTime = formatCompletionTime(deployment);

        // 배포 결과 매핑
        this.result = determineDeploymentResult(deployment.getStatus(), deployment.getIsDeployed());
    }

    /**
     * 기안자 이름 조회
     */
    private String getDrafterName(Deployment deployment) {
        return deployment.getIssuer() != null ? deployment.getIssuer().getName() : null;
    }

    /**
     * 부서명 조회
     */
    private String getDepartmentName(Deployment deployment) {
        if (deployment.getIssuer() != null && deployment.getIssuer().getDepartment() != null) {
            return deployment.getIssuer().getDepartment().getKoreanName();
        }
        return null;
    }

    /**
     * 서비스명 조회
     */
    private String getServiceName(Deployment deployment) {
        return deployment.getProject() != null ? deployment.getProject().getName() : null;
    }

    /**
     * 작업 관리 목록에 표시할 "작업 단계" 결정
     *
     * 규칙:
     * 1. stage == PLAN: "계획서" (배포/재배포/복구 구분 없이 모두 "계획서")
     * 2. stage == RETRY: "재배포"
     * 3. stage == ROLLBACK: "복구"
     * 4. stage == REPORT: "결과보고"
     * 5. stage == DRAFT: "임시저장"
     * 6. 그 외 (또는 type 사용): deployment.type에 따라 결정
     *    - DEPLOY: "배포"
     *    - REDEPLOY: "재배포"
     *    - ROLLBACK: "복구"
     */
    private String determineDisplayStage(Deployment deployment) {
        DeploymentStage stage = deployment.getStage();
        DeploymentType type = deployment.getType();

        // stage가 있는 경우 우선 사용
        if (stage != null) {
            switch (stage) {
                case PLAN:
                    return "계획서";
                case RETRY:
                    return "재배포";
                case ROLLBACK:
                    return "복구";
                case REPORT:
                    return "결과보고";
                case DRAFT:
                    return "임시저장";
                default:
                    break;
            }
        }

        // stage가 없거나 예외 케이스: type으로 결정
        if (type != null) {
            switch (type) {
                case DEPLOY:
                    return "배포";
                case REDEPLOY:
                    return "재배포";
                case ROLLBACK:
                    return "복구";
                default:
                    return type.getKoreanName();
            }
        }

        return null;
    }

    /**
     * 작업 관리 목록에 표시할 "작업 상태" 결정
     *
     * 규칙:
     * - PENDING: "대기" (계획서 승인 대기 또는 결과보고 승인 대기)
     * - APPROVED: "승인"
     * - IN_PROGRESS: "진행중"
     * - COMPLETED: "완료"
     * - REJECTED: "반려"
     * - CANCELED: "취소"
     */
    private String determineDisplayStatus(Deployment deployment) {
        DeploymentStatus status = deployment.getStatus();

        if (status == null) {
            return null;
        }

        return status.getKoreanName();
    }

    /**
     * 완료 시각 포맷팅 (YYYY.MM.DD HH:mm)
     */
    private String formatCompletionTime(Deployment deployment) {
        LocalDateTime completionTime = null;

        // 배포 완료 시각 또는 업데이트 시각 사용
        if (deployment.getStatus() == DeploymentStatus.COMPLETED) {
            completionTime = deployment.getUpdatedAt();
        } else if (deployment.getScheduledToEndedAt() != null) {
            completionTime = deployment.getScheduledToEndedAt();
        }

        if (completionTime != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
            return completionTime.format(formatter);
        }

        return null;
    }

    /**
     * 배포 결과 결정 (성공/실패/null)
     */
    private String determineDeploymentResult(DeploymentStatus deploymentStatus, Boolean isDeployed) {
        if (deploymentStatus == null) {
            return null;
        }

        // COMPLETED 상태이고 isDeployed 값이 있는 경우
        if (deploymentStatus == DeploymentStatus.COMPLETED && isDeployed != null) {
            return isDeployed ? "성공" : "실패";
        }

        return null;
    }
}
