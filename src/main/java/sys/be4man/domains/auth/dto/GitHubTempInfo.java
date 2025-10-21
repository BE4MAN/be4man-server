package sys.be4man.domains.auth.dto;

import java.io.Serializable;

/**
 * OAuth 성공 시 GitHub 정보를 Redis에 임시 저장하기 위한 DTO SignToken에는 githubId만 포함하고, 나머지 정보는 Redis에 저장 TTL: 5분
 * (SignToken과 동일)
 *
 * @param email             GitHub 이메일 (nullable - GitHub에서 이메일을 비공개로 설정한 경우 null)
 * @param githubAccessToken GitHub Access Token (필수)
 * @param profileImageUrl   GitHub 프로필 이미지 URL (nullable)
 */
public record GitHubTempInfo(
        String email,
        String githubAccessToken,
        String profileImageUrl
) implements Serializable {

}

