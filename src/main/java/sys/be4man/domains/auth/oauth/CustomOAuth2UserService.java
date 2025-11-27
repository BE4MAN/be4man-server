// 작성자 : 이원석
package sys.be4man.domains.auth.oauth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * [Step 1] GitHub OAuth2 사용자 정보를 가져오는 서비스
 * <p>
 * Flow: 5. Backend가 GitHub code를 access token으로 교환 (super.loadUser) 6. Backend가 GitHub access
 * token으로 사용자 정보 조회 (super.loadUser) 7. GitHub Access Token을 attributes에 추가하여 OAuth2SuccessHandler로
 * 전달
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String githubAccessToken = userRequest.getAccessToken().getTokenValue();

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("githubAccessToken", githubAccessToken);
        log.info("GitHub OAuth2 loadUser - login: {}", attributes.get("login"));

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_GUEST")),
                attributes,
                "login"
        );
    }
}

