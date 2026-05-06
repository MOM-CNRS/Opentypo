package fr.cnrs.opentypo.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration de l'API Zotero (bibliothèque de groupe du projet).
 */
@Component
@ConfigurationProperties(prefix = "opentypo.zotero")
@Getter
@Setter
public class ZoteroProperties {

    /** Identifiant numérique du groupe Zotero (ex. opentypo). */
    private long groupId = 6519271L;

    /**
     * Clé API (recommandée en prod pour les quotas).
     * Création : https://www.zotero.org/settings/keys — variable {@code OPENTYPO_ZOTERO_API_KEY}.
     */
    private String apiKey = "";

    /** Style CSL (sans extension), cf. dépôt Zotero CSL. */
    private String citationStyle = "chicago-note-bibliography";

    /** Locale CSL (ex. fr-FR). */
    private String locale = "fr-FR";
}
