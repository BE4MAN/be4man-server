package sys.be4man.domains.dashboard.dto.response;

/**
 * 관련 서비스 정보 응답 DTO
 *
 * @param id        서비스 ID (project ID)
 * @param name      서비스 이름
 * @param projectId 프로젝트 ID
 */
public record RelatedServiceResponse(
        Long id,
        String name,
        Long projectId
) {
}




