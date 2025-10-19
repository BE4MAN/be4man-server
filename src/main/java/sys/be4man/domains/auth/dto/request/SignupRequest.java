package sys.be4man.domains.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import sys.be4man.domains.account.model.type.AccountPosition;

/**
 * 회원가입 요청 DTO
 */
public record SignupRequest(
        @NotBlank(message = "이름은 필수입니다")
        String name,

        String department,

        @NotNull(message = "직급은 필수입니다")
        AccountPosition position
) {

}
