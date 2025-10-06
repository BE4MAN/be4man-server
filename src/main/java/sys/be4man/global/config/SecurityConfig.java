package sys.be4man.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)

            // 공개 목록
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                        "/health"

                        // swagger
                        , "/v3/api-docs/**"
                        , "/swagger-ui/**"
                        , "/swagger-ui.html"
                        , "/swagger-resources/**"
                        , "/webjars/**"
                        , "/swagger-ui/index.html"

                        // 인증 (login, logout)

                        // h2 콘솔

                        //

                    )
                    .permitAll()

                    .anyRequest().authenticated()

                // 인가 - 권한 분기 처리

            );
        return http.build();
    }
}