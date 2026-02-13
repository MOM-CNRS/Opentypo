package fr.cnrs.opentypo.presentation.bean.candidats.service;

import org.springframework.stereotype.Service;

/**
 * Fournit les messages et paramètres pour la boîte de confirmation de suppression unique.
 */
@Service
public class CandidatConfirmDeleteService {

    private static final String MSG_REF = "Cette action supprimera définitivement la référence de la base de données.";

    public record ConfirmConfig(String type, String message, String update) {}

    public ConfirmConfig getConfig(String type) {
        return switch (type) {
            case "PERIODE" -> new ConfirmConfig("PERIODE",
                "Êtes-vous sûr de vouloir supprimer la période sélectionnée ? Cette action supprimera définitivement la référence et retirera la période de l'entité.",
                ":createCandidatForm:periode :growl");
            case "PRODUCTION" -> new ConfirmConfig("PRODUCTION",
                "Êtes-vous sûr de vouloir supprimer la production sélectionnée ? Cette action supprimera définitivement la référence et retirera la production de l'entité.",
                ":createCandidatForm:productionGroup :growl");
            case "AIRE_CIRCULATION" -> new ConfirmConfig("AIRE_CIRCULATION",
                "Êtes-vous sûr de vouloir supprimer cette aire de circulation ? Cette action supprimera définitivement la référence et retirera l'aire de circulation de l'entité.",
                ":createCandidatForm :growl");
            case "FONCTION_USAGE" -> new ConfirmConfig("FONCTION_USAGE",
                "Êtes-vous sûr de vouloir supprimer cette fonction/usage ? " + MSG_REF,
                ":createCandidatForm :growl");
            case "METROLOGIE" -> new ConfirmConfig("METROLOGIE",
                "Êtes-vous sûr de vouloir supprimer la métrologie sélectionnée ? " + MSG_REF,
                ":createCandidatForm :growl");
            case "FABRICATION_FACONNAGE" -> new ConfirmConfig("FABRICATION_FACONNAGE",
                "Êtes-vous sûr de vouloir supprimer la fabrication/façonnage sélectionné ? " + MSG_REF,
                ":createCandidatForm :growl");
            case "COULEUR_PATE" -> new ConfirmConfig("COULEUR_PATE",
                "Êtes-vous sûr de vouloir supprimer la couleur de pâte sélectionnée ? " + MSG_REF,
                ":createCandidatForm :growl");
            case "NATURE_PATE" -> new ConfirmConfig("NATURE_PATE",
                "Êtes-vous sûr de vouloir supprimer la nature de pâte sélectionnée ? " + MSG_REF,
                ":createCandidatForm :growl");
            case "INCLUSIONS" -> new ConfirmConfig("INCLUSIONS",
                "Êtes-vous sûr de vouloir supprimer les inclusions sélectionnées ? " + MSG_REF,
                ":createCandidatForm :growl");
            case "CUISSON_POST_CUISSON" -> new ConfirmConfig("CUISSON_POST_CUISSON",
                "Êtes-vous sûr de vouloir supprimer la cuisson/post-cuisson sélectionnée ? " + MSG_REF,
                ":createCandidatForm :growl");
            case "MATERIAU" -> new ConfirmConfig("MATERIAU", "Êtes-vous sûr de vouloir supprimer le matériau ? " + MSG_REF, ":createCandidatForm :growl");
            case "DENOMINATION" -> new ConfirmConfig("DENOMINATION", "Êtes-vous sûr de vouloir supprimer la dénomination ? " + MSG_REF, ":createCandidatForm :growl");
            case "VALEUR" -> new ConfirmConfig("VALEUR", "Êtes-vous sûr de vouloir supprimer la valeur ? " + MSG_REF, ":createCandidatForm :growl");
            case "TECHNIQUE" -> new ConfirmConfig("TECHNIQUE", "Êtes-vous sûr de vouloir supprimer la technique ? " + MSG_REF, ":createCandidatForm :growl");
            case "FABRICATION_MONNAIE" -> new ConfirmConfig("FABRICATION_MONNAIE", "Êtes-vous sûr de vouloir supprimer la fabrication ? " + MSG_REF, ":createCandidatForm :growl");
            default -> null;
        };
    }
}
