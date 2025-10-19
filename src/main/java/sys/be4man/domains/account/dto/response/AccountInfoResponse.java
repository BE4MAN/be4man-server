package sys.be4man.domains.account.dto.response;

import sys.be4man.domains.account.model.entity.Account;
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

    /**
     * Account 엔티티로부터 AccountInfoResponse 생성
     */
    public static AccountInfoResponse from(Account account) {
        return new AccountInfoResponse(
                account.getId(),
                account.getEmail(),
                account.getName(),
                account.getProfileImageUrl(),
                account.getRole(),
                account.getPosition(),
                account.getDepartment()
        );
    }
}

