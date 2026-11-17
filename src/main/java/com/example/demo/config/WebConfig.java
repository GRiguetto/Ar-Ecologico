package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serve o front-end pela mesma porta da API (http://localhost:8080), mantendo a
 * estrutura de pastas atual de src/main/resources:
 *
 *   /                    -> redireciona para /index.html
 *   /index.html, /login.html  -> src/main/resources/ (via PaginasController)
 *   /templates/**        -> src/main/resources/templates/
 *   /static/**           -> src/main/resources/static/
 *
 * Só esses caminhos são expostos (application.properties e afins continuam privados).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /index.html e /login.html ficam na raiz do classpath e são servidos pelo PaginasController.
        registry.addResourceHandler("/templates/**").addResourceLocations("classpath:/templates/");
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/index.html");
    }

    /** Permite abrir as telas por outro servidor local (ex.: Live Server na porta 5500). */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
