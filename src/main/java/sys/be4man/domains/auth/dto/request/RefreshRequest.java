// 작성자 : 이원석
package sys.be4man.domains.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 갱신 요청 DTO
 */
public record RefreshRequest(
        @NotBlank(message = "Refresh Token은 필수입니다")
        String refreshToken
) {

}

