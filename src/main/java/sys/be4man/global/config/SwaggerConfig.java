// 작성자 : 이원석
package sys.be4man.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String JWT_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT access 토큰을 입력해주세요!");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(JWT_SCHEME_NAME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME_NAME))
                .info(new Info()
                              .title("배포맨 API 명세서")
                              .description(
                                      "\uD83E\uDD16 Jenkins Github 배포 작업 관리 서비스 배포맨 API 명세서입니다.")
                              .version("v1.0.0")
                              .contact(new Contact()
                                               .name("배포맨 github")
                                               .url("https://github.com/BE4MAN"))
                );
    }
}
