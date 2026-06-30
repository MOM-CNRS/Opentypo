package fr.cnrs.opentypo.application.import_typology;

import fr.cnrs.opentypo.domain.entity.AuteurScientifique;
import fr.cnrs.opentypo.infrastructure.persistence.AuteurScientifiqueRepository;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Cache nom/prénom → auteur scientifique pour éviter les requêtes répétées pendant l'import.
 */
final class TypologyImportAuthorCache {

    private static final String KEY_SEP = "\u001F";

    private final AuteurScientifiqueRepository auteurScientifiqueRepository;
    private final Map<String, AuteurScientifique> byKey = new HashMap<>();

    TypologyImportAuthorCache(AuteurScientifiqueRepository auteurScientifiqueRepository) {
        this.auteurScientifiqueRepository = auteurScientifiqueRepository;
    }

    AuteurScientifique resolve(String nom, String prenom) {
        String key = authorKey(nom, prenom);
        return byKey.computeIfAbsent(key, k -> auteurScientifiqueRepository
                .findFirstByNomIgnoreCaseAndPrenomIgnoreCaseOrderByIdAsc(nom, prenom)
                .orElseGet(() -> {
                    AuteurScientifique created = new AuteurScientifique();
                    created.setNom(nom);
                    created.setPrenom(prenom);
                    created.setActive(true);
                    return auteurScientifiqueRepository.save(created);
                }));
    }

    private static String authorKey(String nom, String prenom) {
        return nom.toLowerCase(Locale.ROOT) + KEY_SEP + prenom.toLowerCase(Locale.ROOT);
    }
}
