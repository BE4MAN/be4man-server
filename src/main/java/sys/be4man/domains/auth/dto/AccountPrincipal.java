// 작성자 : 이원석
package sys.be4man.domains.auth.dto;

import sys.be4man.domains.account.model.type.Role;

/**
 * Spring Security Authentication Principal JWT에서 추출한 계정 정보를 담는 객체
 */
public record AccountPrincipal(
        Long accountId,
        Role role
) {

}

