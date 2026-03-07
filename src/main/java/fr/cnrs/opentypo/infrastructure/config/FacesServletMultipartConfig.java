package fr.cnrs.opentypo.infrastructure.config;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ajoute MultipartConfig au FacesServlet pour que h:inputFile fonctionne.
 */
@Configuration
public class FacesServletMultipartConfig {

    private static final String[] FACES_SERVLET_NAMES = {"Faces Servlet", "FacesServlet", "faces"};
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;      // 10 MB
    private static final long MAX_REQUEST_SIZE = 10 * 1024 * 1024;  // 10 MB
    private static final int FILE_SIZE_THRESHOLD = 0;

    @Bean
    public ServletContextInitializer facesServletMultipartInitializer() {
        return servletContext -> {
            ServletRegistration.Dynamic facesServlet = findFacesServlet(servletContext);
            if (facesServlet != null) {
                MultipartConfigElement multipartConfig = new MultipartConfigElement(
                        null, MAX_FILE_SIZE, MAX_REQUEST_SIZE, FILE_SIZE_THRESHOLD);
                facesServlet.setMultipartConfig(multipartConfig);
            }
        };
    }

    private ServletRegistration.Dynamic findFacesServlet(ServletContext context) {
        for (ServletRegistration registration : context.getServletRegistrations().values()) {
            String name = registration.getName();
            for (String facesName : FACES_SERVLET_NAMES) {
                if (facesName.equals(name)) {
                    return (ServletRegistration.Dynamic) registration;
                }
            }
            if (name != null && name.toLowerCase().contains("faces")) {
                return (ServletRegistration.Dynamic) registration;
            }
        }
        return null;
    }
}
