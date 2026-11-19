package ar.edu.taller.config;

import ar.edu.taller.service.FotoStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración web:
 *  - Sirve las fotos subidas como recurso estático en /uploads/**.
 *  - Habilita CORS para el frontend de Vite (localhost:5173 y previews).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FotoStorageService fotos;
    private final String allowedOrigins;

    public WebConfig(FotoStorageService fotos,
                     @Value("${app.cors.allowed-origins:*}") String allowedOrigins) {
        this.fotos = fotos;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // file:<ruta absoluta>/ → /uploads/<archivo>
        String ubicacion = fotos.getRaiz().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(ubicacion.endsWith("/") ? ubicacion : ubicacion + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
        registry.addMapping("/uploads/**")
                .allowedOriginPatterns(allowedOrigins.split(","));
    }
}
