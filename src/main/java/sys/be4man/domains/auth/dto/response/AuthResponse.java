package sys.be4man.domains.auth.dto.response;

/**
 * 인증 성공 시 반환되는 응답 DTO
 * Access Token과 Refresh Token 포함 (Bearer 타입)
 */
public record AuthResponse(
        String accessToken,
        String refreshToken
) {

}
