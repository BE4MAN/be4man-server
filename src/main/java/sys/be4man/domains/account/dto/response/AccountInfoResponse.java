package sys.be4man.domains.account.dto.response;

import sys.be4man.domains.account.model.type.AccountPosition;
import sys.be4man.domains.account.model.type.Role;

/**
 * 계정 정보 응답 DTO
 */
public record AccountInfoResponse(
        Long id,
        String email,
        String name,
        String profileImageUrl,
        Role role,
        AccountPosition position,
        String department
) {

}

