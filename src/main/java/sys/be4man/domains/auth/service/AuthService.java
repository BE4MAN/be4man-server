package sys.be4man.domains.auth.service;

import sys.be4man.domains.account.model.type.AccountPosition;
import sys.be4man.domains.auth.dto.response.AuthResponse;

/**
 * 인증 관련 비즈니스 로직 인터페이스
 */
public interface AuthService {

    /**
     * SignToken을 사용하여 신규 계정 생성
     */
    AuthResponse signup(String signToken, String name,
            String department, AccountPosition position);

    /**
     * Access Token으로 사용자 조회 후 새로운 Access Token 발급
     */
    AuthResponse refresh(String accessToken);

    /**
     * Access Token으로 사용자 조회 후 Refresh Token 삭제 (로그아웃)
     */
    void logout(String authorization);
}
