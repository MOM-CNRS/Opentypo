package fr.cnrs.opentypo.conf;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Contrôleur pour gérer les erreurs HTTP (404, 500, etc.)
 * et rediriger vers la page d'erreur dédiée
 */
@Controller
public class ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(ErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            // Logger l'erreur
            if (exception != null) {
                logger.error("Erreur HTTP {} : {}", statusCode, exception);
            } else {
                logger.error("Erreur HTTP {} : {}", statusCode, message != null ? message : "Aucun message");
            }
            
            // Pour les erreurs 404, on peut rediriger vers index.xhtml
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "redirect:/index.xhtml";
            }
        }
        
        // Pour toutes les autres erreurs, rediriger vers error.xhtml
        String errorMessage = "Une erreur technique s'est produite";
        if (message != null) {
            errorMessage = message.toString();
        } else if (exception != null) {
            errorMessage = exception.getClass().getSimpleName();
        }
        
        // Encoder le message pour l'URL
        try {
            String encodedMessage = java.net.URLEncoder.encode(errorMessage, "UTF-8");
            return "redirect:/error.xhtml?message=" + encodedMessage;
        } catch (Exception e) {
            logger.error("Erreur lors de l'encodage du message d'erreur", e);
            return "redirect:/error.xhtml";
        }
    }
}

