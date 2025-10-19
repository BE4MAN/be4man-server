package sys.be4man.domains.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO OAuth 인증 후 발급된 임시 코드를 통해 토큰 발급
 */
public record SigninRequest(
        @NotBlank(message = "임시 코드는 필수입니다")
        String code
) {

}

