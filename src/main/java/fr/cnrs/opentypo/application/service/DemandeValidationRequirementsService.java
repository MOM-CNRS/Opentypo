package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Contrôle les champs obligatoires pour une « demande de validation » selon la typologie de collection.
 */
@Service
public class DemandeValidationRequirementsService {

    public enum TypologyProfile {
        CERAMIQUE,
        MONNAIE,
        INSTRUMENTUM,
        UNSUPPORTED
    }

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private CollectionService collectionService;

    /**
     * @return liste vide si tout est renseigné ; sinon libellés des champs manquants (pour messages utilisateur).
     */
    @Transactional(readOnly = true)
    public List<String> computeMissingRequiredLabels(Long entityId) {
        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null) {
            return List.of("Entité introuvable.");
        }
        if (entity.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_TYPE.equals(entity.getEntityType().getCode())) {
            return List.of("La demande de validation ne concerne que les fiches de type « Type ».");
        }

        TypologyProfile profile = resolveProfile(entity);
        if (profile == TypologyProfile.UNSUPPORTED) {
            return List.of(
                    "Typologie de collection non reconnue (attendu : Céramique, Monnaie ou Instrumentum).");
        }

        List<String> missing = new ArrayList<>();
        switch (profile) {
            case CERAMIQUE -> collectCeramique(entity, missing);
            case MONNAIE -> collectMonnaie(entity, missing);
            case INSTRUMENTUM -> collectInstrumentum(entity, missing);
            default -> {
            }
        }
        return missing;
    }

    public TypologyProfile resolveProfile(Entity entity) {
        Entity collection = collectionService.findCollectionIdByEntityId(entity.getId());
        if (collection == null) {
            return TypologyProfile.UNSUPPORTED;
        }
        String lab = collection.getNom("fr");
        if (lab == null || lab.isBlank()) {
            lab = collection.getNom();
        }
        if (lab == null) {
            lab = "";
        }
        lab = lab.trim();
        if (lab.isEmpty() && collection.getCode() != null) {
            lab = collection.getCode().trim();
        }
        if ("MONNAIE".equalsIgnoreCase(lab) || "CASH".equalsIgnoreCase(lab)) {
            return TypologyProfile.MONNAIE;
        }
        String u = lab.toUpperCase(Locale.ROOT);
        if (u.contains("CERAMIQUE") || u.contains("CERAMIC")) {
            return TypologyProfile.CERAMIQUE;
        }
        if (u.contains("INSTRUMENTUM")) {
            return TypologyProfile.INSTRUMENTUM;
        }
        return TypologyProfile.UNSUPPORTED;
    }

    private static void collectCeramique(Entity entity, List<String> missing) {
        if (!textPresent(entity.getCode())) {
            missing.add("Code");
        }
        if (entity.getImages() == null || entity.getImages().isEmpty()) {
            missing.add("Image(s) : au moins une image est requise");
        }
        if (entity.getTpq() == null) {
            missing.add("TPQ");
        }
        if (entity.getTaq() == null) {
            missing.add("TAQ");
        }
        if (!hasAireCirculation(entity)) {
            missing.add("Aire de circulation (au moins une valeur)");
        }
        if (!semicolonListPresent(entity.getAttestations())) {
            missing.add("Attestations (au moins une valeur)");
        }
        if (!textPresent(entity.getTypologieScientifique())) {
            missing.add("Typologie scientifique");
        }
        if (!textPresent(entity.getInterne())) {
            missing.add("Alignement interne");
        }
    }

    private static void collectMonnaie(Entity entity, List<String> missing) {
        if (!semicolonListPresent(entity.getAttestations())) {
            missing.add("Attestations (au moins une valeur)");
        }
        if (!semicolonListPresent(entity.getSitesArcheologiques())) {
            missing.add("Sites archéologiques (au moins une valeur)");
        }
        if (!textPresent(entity.getReference())) {
            missing.add("Référentiel");
        }
        if (!textPresent(entity.getTypologieScientifique())) {
            missing.add("Typologie scientifique");
        }
        if (!textPresent(entity.getCode())) {
            missing.add("Code");
        }
        if (entity.getImages() == null || entity.getImages().isEmpty()) {
            missing.add("Image(s) : au moins une image est requise");
        }
        if (entity.getPeriode() == null) {
            missing.add("Période");
        }
        if (entity.getTpq() == null) {
            missing.add("TPQ");
        }
        if (entity.getTaq() == null) {
            missing.add("TAQ");
        }
        if (entity.getProduction() == null) {
            missing.add("Production");
        }
        if (!hasAireCirculation(entity)) {
            missing.add("Aire de circulation (au moins une valeur)");
        }
        var dm = entity.getDescriptionMonnaie();
        if (dm == null || !textPresent(dm.getDroit())) {
            missing.add("Droit");
        }
        if (dm == null || !textPresent(dm.getRevers())) {
            missing.add("Revers");
        }
        var cpm = entity.getCaracteristiquePhysiqueMonnaie();
        if (cpm == null || cpm.getMateriaux() == null) {
            missing.add("Matériau");
        }
    }

    private static void collectInstrumentum(Entity entity, List<String> missing) {
        if (!textPresent(entity.getCode())) {
            missing.add("Code");
        }
        if (entity.getPeriode() == null) {
            missing.add("Période");
        }
        if (entity.getTpq() == null) {
            missing.add("TPQ");
        }
        if (entity.getTaq() == null) {
            missing.add("TAQ");
        }
        if (entity.getProduction() == null) {
            missing.add("Production");
        }
        if (!semicolonListPresent(entity.getAteliers())) {
            missing.add("Atelier(s) (au moins une valeur)");
        }
        if (!hasAireCirculation(entity)) {
            missing.add("Aire de circulation (au moins une valeur)");
        }
        if (entity.getCategorieFonctionnelle() == null) {
            missing.add("Catégorie fonctionnelle");
        }
        var cp = entity.getCaracteristiquePhysique();
        if (cp == null || cp.getMateriaux() == null) {
            missing.add("Matériau");
        }
    }

    private static boolean hasAireCirculation(Entity entity) {
        if (entity.getAiresCirculation() == null || entity.getAiresCirculation().isEmpty()) {
            return false;
        }
        return entity.getAiresCirculation().stream()
                .filter(r -> ReferenceOpenthesoEnum.AIRE_CIRCULATION.name().equals(r.getCode()))
                .anyMatch(r -> textPresent(r.getValeur()));
    }

    private static boolean textPresent(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * Liste séparée par « ; » avec au moins un segment non vide.
     */
    private static boolean semicolonListPresent(String stored) {
        if (!textPresent(stored)) {
            return false;
        }
        return Arrays.stream(stored.split(";"))
                .map(String::trim)
                .anyMatch(s -> !s.isEmpty());
    }
}
