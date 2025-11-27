// 작성자 : 김민호, 이원석
package sys.be4man.domains.account.dto.response;

import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.model.type.JobDepartment;
import sys.be4man.domains.account.model.type.JobPosition;
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
        JobPosition position,
        JobDepartment department,
        Long githubId
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
                account.getDepartment(),
                account.getGithubId()
        );
    }
}

