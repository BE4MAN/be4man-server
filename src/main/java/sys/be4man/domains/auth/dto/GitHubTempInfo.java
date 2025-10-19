package sys.be4man.domains.auth.dto;

import java.io.Serializable;

/**
 * OAuth 성공 시 GitHub 정보를 Redis에 임시 저장하기 위한 DTO SignToken에는 githubId만 포함하고, 나머지 정보는 Redis에 저장
 */
public record GitHubTempInfo(
        String email,
        String githubAccessToken,
        String profileImageUrl
) implements Serializable {

}

