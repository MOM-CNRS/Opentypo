package fr.cnrs.opentypo.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration des identifiants ARK (Archival Resource Key) générés à la publication des typologies.
 */
@Component
@ConfigurationProperties(prefix = "opentypo.ark")
@Getter
@Setter
public class OpentypoArkProperties {

    /**
     * Active l'attribution automatique d'un ARK lorsque le statut devient {@code PUBLIQUE}
     * pour un groupe, une série ou un type.
     */
    private boolean enabled = true;

    /**
     * NAAN (Name Assigning Authority Number). Vide par défaut : aucun ARK n'est attribué tant que le NAAN
     * n'est pas renseigné (référentiel ou variable {@code OPENTYPO_ARK_NAAN}).
     */
    private String naan = "";

    /**
     * Épaule (shoulder) après le NAAN. Vide par défaut : aucun ARK sans épaule explicite
     * (référentiel ou {@code OPENTYPO_ARK_SHOULDER}).
     */
    private String shoulder = "";

}
