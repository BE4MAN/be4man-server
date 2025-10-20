package sys.be4man.domains.account.service;

import sys.be4man.domains.account.dto.response.AccountInfoResponse;

/**
 * 계정 관련 비즈니스 로직 인터페이스
 */
public interface AccountService {

    /**
     * 내 계정 정보 조회
     */
    AccountInfoResponse getMyAccount(Long accountId);
}
