package sys.be4man.domains.auth.dto.response;

/**
 * 인증 성공 시 반환되는 응답 DTO Access Token만 포함
 */
public record AuthResponse(
        String accessToken,
        String tokenType
) {

}
