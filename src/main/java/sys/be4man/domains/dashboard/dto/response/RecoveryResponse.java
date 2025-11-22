package sys.be4man.domains.dashboard.dto.response;

import java.time.LocalDateTime;

/**
 * 복구현황 응답 DTO
 *
 * @param id                   복구현황 고유 ID (deployment ID)
 * @param title                배포작업명 (deployment.title)
 * @param service              서비스명 (deployment.projectName)
 * @param status               복구 상태 (PENDING, IN_PROGRESS, COMPLETED)
 * @param duration             소요시간 (예: "42분") - status가 COMPLETED일 때만 제공
 * @param buildRunDuration     BuildRun duration (초 단위) - 마지막 BuildRun의 duration
 * @param recoveredAt          복구 완료 시각 (ISO 8601 형식) - deployment.updatedAt
 * @param updatedAt            업데이트 시각 (ISO 8601 형식) - deployment.updatedAt
 * @param registrant           등록자 이름 (deployment.issuer.name)
 * @param registrantDepartment 등록 부서명 (deployment.issuer.department.koreanName)
 * @param deploymentId         원본 Deployment ID
 */
public record RecoveryResponse(
        Long id,
        String title,
        String service,
        String status,
        String duration,
        Integer buildRunDuration,
        LocalDateTime recoveredAt,
        LocalDateTime updatedAt,
        String registrant,
        String registrantDepartment,
        Long deploymentId
) {
}

