package fr.cnrs.opentypo.application.import_typology;

import java.io.Serializable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache des résultats de validation d'URL d'image (analyse réseau) réutilisé à l'import
 * pour éviter les requêtes HTTP répétées.
 */
public final class TypologyImportImageUrlCache implements Serializable {

    private final Map<String, Boolean> networkResults = new HashMap<>();

    boolean validateWithNetwork(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return false;
        }
        String key = rawUrl.trim();
        return networkResults.computeIfAbsent(key, RemoteImageUrlValidator::isValidRemoteImageUrl);
    }

    /**
     * À l'import : réutilise le résultat réseau de l'analyse, sinon validation syntaxique uniquement.
     */
    boolean isValidForImport(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return false;
        }
        String key = rawUrl.trim();
        Boolean cached = networkResults.get(key);
        if (cached != null) {
            return cached;
        }
        return RemoteImageUrlValidator.isSyntaxValidUrl(key);
    }
}
