package sys.be4man.global.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import sys.be4man.domains.auth.jwt.JwtAuthenticationFilter;
import sys.be4man.domains.auth.oauth.CustomOAuth2UserService;
import sys.be4man.domains.auth.oauth.OAuth2SuccessHandler;

/**
 * Spring Security 설정 JWT 기반 인증과 OAuth2 로그인을 설정합니다. Spring Security 6.3.x 기준으로 작성되었습니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public static final String[] WHITE_LIST = {
            // 헬스 체크
            "/api/health",

            // Swagger UI
            "/api-docs/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/swagger-resources/**",
            "/webjars/**",

            // H2 Console (개발 환경)
            "/h2-console/**",

            // OAuth2 관련 경로
            "/oauth/**",
            "/oauth2/**",
            "/login/oauth2/**",

            // 인증 API (인증 불필요)
            "/api/auth/signup",
            "/api/auth/signin",
            "/api/auth/refresh",

            // 공개 API
            "/public/**"
    };

    /**
     * Security Filter Chain 설정 JWT 인증 필터를 추가하고, 경로별 접근 권한을 설정합니다.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용으로 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정
                .cors(Customizer.withDefaults())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Form Login 비활성화 (JWT 사용)
                .formLogin(AbstractHttpConfigurer::disable)

                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)

                // 로그아웃 비활성화 (커스텀 로그아웃 엔드포인트 사용)
                .logout(AbstractHttpConfigurer::disable)

                // 세션 관리: Stateless 모드 (JWT 사용으로 세션 불필요)
                .sessionManagement(session ->
                                           session.sessionCreationPolicy(
                                                   SessionCreationPolicy.STATELESS)
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // OAuth2 사용자 정보를 가져오는 서비스 설정
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        // OAuth2 로그인 성공 시 호출될 핸들러 설정
                        .successHandler(oAuth2SuccessHandler)
                )

                // JWT 인증 필터 추가 (UsernamePasswordAuthenticationFilter 앞에 배치)
                .addFilterBefore(jwtAuthenticationFilter,
                                 UsernamePasswordAuthenticationFilter.class)

                // 경로별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                                               // 공개 경로: 인증 없이 접근 가능
                                               // TODO: 구현 후 WHITE_LIST 적용
                                               .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                               .anyRequest().permitAll()

                                       // 나머지 모든 요청은 인증 필요
//                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /**
     * CORS 설정 크로스 도메인 요청을 허용합니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 프론트엔드 도메인 허용 (allowCredentials(true)와 함께 사용 시 구체적 도메인 필요)
        configuration.setAllowedOrigins(List.of(
                "https://be4man-client.vercel.app",
                "http://localhost:5173",  // 로컬 개발 환경 (Vite 기본 포트)
                "http://localhost:3000"   // 로컬 개발 환경 (대체 포트)
        ));
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        );
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L); // Preflight 요청 캐시 시간 (1시간)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}


