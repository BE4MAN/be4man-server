// 작성자 : 김민호
package sys.be4man.domains.account.dto.response;

import lombok.Builder;
import lombok.Getter;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.model.type.JobDepartment;
import sys.be4man.domains.account.model.type.JobPosition;

@Getter
@Builder
public class ApprovalLineAccountResponse {

    private Long accountId;
    private String name;
    private String email;

    private JobDepartment department;
    private JobPosition position;

    public static ApprovalLineAccountResponse from(Account account) {
        if (account == null) {
            return null;
        }

        return ApprovalLineAccountResponse.builder()
                .accountId(account.getId())
                .name(account.getName())
                .email(account.getEmail())
                .department(account.getDepartment())
                .position(account.getPosition())
                .build();
    }
}
