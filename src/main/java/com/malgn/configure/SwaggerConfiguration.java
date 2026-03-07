package com.malgn.configure;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title       = "CMS API",
                description = "콘텐츠 관리 시스템 REST API.\n\n" +
                              "**인증 방법**: 먼저 `/api/auth/login`으로 JWT 토큰을 발급받은 뒤, " +
                              "우측 상단 **Authorize** 버튼을 클릭하여 토큰을 입력하세요.",
                version     = "v1"
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name        = "bearerAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat = "JWT",
        description = "로그인 후 발급된 JWT 토큰을 입력하세요. (Bearer 접두사 제외)"
)
@Configuration
public class SwaggerConfiguration {
}
