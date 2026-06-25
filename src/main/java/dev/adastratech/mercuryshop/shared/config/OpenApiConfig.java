package dev.adastratech.mercuryshop.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Metadados do OpenAPI + esquema de segurança Bearer (JWT), para o Swagger UI mostrar o botão
 * "Authorize" e enviar o header Authorization nos endpoints protegidos. Ativo apenas no perfil dev.
 */
@Configuration
@Profile("dev")
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI mercuryShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mercury Shop API")
                        .version("v1")
                        .description("E-commerce / Order Management API. "
                                + "Faça login em /v1/auth/login, copie o accessToken e clique em "
                                + "'Authorize' para chamar os endpoints protegidos."))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
