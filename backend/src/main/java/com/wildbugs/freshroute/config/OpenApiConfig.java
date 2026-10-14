package com.wildbugs.freshroute.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FreshRoute: Decision Support API for Perishable Produce Distribution")
                        .version("1.0.0")
                        .description("Automated FEFO-based perishable produce allocation, dynamic discounting, and disruption management system. " +
                                "Engineered to minimize post-harvest food waste in Indian horticultural corridors according to MoFPI guidelines.")
                        .contact(new Contact()
                                .name("Nikunj Garg (WildBugs)")
                                .email("gargnikunj991@gmail.com")
                                .url("https://github.com/gargnikunj991-ux/freshroute"))
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")));
    }
}
