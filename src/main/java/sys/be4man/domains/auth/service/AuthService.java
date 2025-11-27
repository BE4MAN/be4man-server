// 작성자 : 이원석
package sys.be4man.domains.auth.service;

import sys.be4man.domains.account.model.type.JobDepartment;
import sys.be4man.domains.account.model.type.JobPosition;
import sys.be4man.domains.auth.dto.response.AuthResponse;

/**
 * 인증 관련 비즈니스 로직 인터페이스
 */
public interface AuthService {

    /**
     * SignToken을 사용하여 신규 계정 생성
     */
    AuthResponse signup(String signToken, String name,
            JobDepartment department, JobPosition position);

    /**
     * OAuth 임시 코드로 로그인 (기존 사용자)
     */
    AuthResponse signin(String tempCode);

    /**
     * Refresh Token으로 새로운 Access Token 및 Refresh Token 발급
     */
    AuthResponse refresh(String refreshToken);

    /**
     * Refresh Token 삭제 (로그아웃)
     */
    void logout(Long accountId);
}
