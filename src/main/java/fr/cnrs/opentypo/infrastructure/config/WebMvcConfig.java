package fr.cnrs.opentypo.infrastructure.config;

import fr.cnrs.opentypo.application.service.EntityImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Configuration Web MVC.
 * Sert les images uploadées depuis le répertoire "images" (à côté du JAR).
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final EntityImageService entityImageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path imagesPath = entityImageService.getImagesBasePath();
        String location = "file:" + imagesPath.toAbsolutePath() + "/";
        registry.addResourceHandler("/uploaded-images/**")
                .addResourceLocations(location);
    }
}
