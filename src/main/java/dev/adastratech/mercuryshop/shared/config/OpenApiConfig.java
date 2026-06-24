package dev.adastratech.mercuryshop.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Metadados do OpenAPI. Ativo apenas no perfil dev (Swagger desligado em prod — seção 7.4). */
@Configuration
@Profile("dev")
public class OpenApiConfig {

    @Bean
    public OpenAPI mercuryShopOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Mercury Shop API")
                .version("v1")
                .description("E-commerce / Order Management API — Fase 1 (catálogo de produtos e categorias)."));
    }
}
