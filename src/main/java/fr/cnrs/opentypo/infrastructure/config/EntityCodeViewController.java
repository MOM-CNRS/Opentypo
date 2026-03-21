package fr.cnrs.opentypo.infrastructure.config;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Set;

/**
 * Contrôleur qui gère les URLs de type /{code} pour afficher directement un élément
 * de la typologie (référentiel, catégorie, groupe, série, type) par son code.
 * Exemple : http://localhost:8080/DECOCER affiche la référence DECOCER.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class EntityCodeViewController {

    private static final Set<String> RESERVED_PATHS = Set.of(
            "index.xhtml", "error.xhtml", "error", "candidats", "profile", "search", "login",
            "users", "details", "dialogs", "tree", "commun", "resources", "webjars",
            "uploaded-images", "session-check", "jakarta.faces.resource", "javax.faces.resource"
    );

    private final EntityRepository entityRepository;

    /**
     * Traite les requêtes GET /{code} pour afficher un élément par son code.
     * Si l'entité existe, transmet le code à la vue index via un attribut de requête
     * et effectue un forward vers index.xhtml. Sinon, redirige vers la racine.
     */
    @GetMapping("/{code}")
    public String handleEntityCode(@PathVariable String code, HttpServletRequest request) {
        if (code == null || code.isBlank()) {
            return "redirect:/";
        }
        if (RESERVED_PATHS.contains(code.toLowerCase())) {
            return "forward:/index.xhtml";
        }
        if (code.contains(".") && (code.endsWith(".xhtml") || code.endsWith(".js") || code.endsWith(".css")
                || code.endsWith(".ico") || code.endsWith(".png") || code.endsWith(".jpg") || code.endsWith(".gif"))) {
            return "forward:/index.xhtml";
        }

        Entity entity = entityRepository.findByCode(code).orElse(null);
        if (entity == null) {
            log.debug("Entité non trouvée pour le code : {}", code);
            return "redirect:/";
        }

        request.setAttribute("entityCodeFromUrl", code);
        return "forward:/index.xhtml";
    }
}
