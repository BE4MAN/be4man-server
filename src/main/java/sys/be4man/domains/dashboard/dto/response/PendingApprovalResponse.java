// 작성자 : 이원석
package sys.be4man.domains.dashboard.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 승인 대기 목록 응답 DTO
 *
 * @param id                   Approval ID
 * @param title                문서 제목
 * @param docType              문서 유형 (작업계획서, 결과보고)
 * @param serviceName          서비스 이름 배열
 * @param requestedAt          요청 시각
 * @param currentApprover      현재 승인 예정자 이름 배열
 * @param registrant           등록자 이름
 * @param registrantDepartment 등록 부서명
 * @param description          설명
 * @param relatedServices      연관 서비스 이름 배열
 * @param status               상태 (승인 대기)
 * @param deployment           Deployment 정보 (nested object)
 */
public record PendingApprovalResponse(
        Long id,
        String title,
        String docType,
        List<String> serviceName,
        LocalDateTime requestedAt,
        List<String> currentApprover,
        String registrant,
        String registrantDepartment,
        String description,
        List<String> relatedServices,
        String status,
        DeploymentInfoResponse deployment
) {
}




