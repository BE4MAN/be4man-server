// 작성자 : 이원석
package sys.be4man.domains.dashboard.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Deployment 정보 응답 DTO (PendingApprovalResponse의 nested object)
 *
 * @param id                   Deployment ID
 * @param title                배포 제목
 * @param status               Deployment 상태
 * @param stage                Deployment 단계
 * @param projectName          프로젝트(서비스) 이름
 * @param scheduledDate        예정 날짜 (YYYY-MM-DD)
 * @param scheduledTime        예정 시간 (HH:mm)
 * @param registrant           등록자 이름
 * @param registrantDepartment 등록 부서명
 * @param relatedServices      연관 서비스 배열
 */
public record DeploymentInfoResponse(
        Long id,
        String title,
        String status,
        String stage,
        String projectName,
        LocalDate scheduledDate,
        LocalTime scheduledTime,
        String registrant,
        String registrantDepartment,
        List<RelatedServiceResponse> relatedServices
) {
}




