package fr.cnrs.opentypo.application.mapper;

import fr.cnrs.opentypo.application.dto.api.EntityDescriptionSectionDto;
import fr.cnrs.opentypo.application.dto.api.EntityPhysicalCharacteristicsSectionDto;
import fr.cnrs.opentypo.application.dto.api.EntityTextDescriptionDto;
import fr.cnrs.opentypo.application.dto.api.OpenThesoReferenceDto;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysique;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysiqueMonnaie;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.DescriptionDetail;
import fr.cnrs.opentypo.domain.entity.DescriptionMonnaie;
import fr.cnrs.opentypo.domain.entity.DescriptionPate;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityMetadata;
import fr.cnrs.opentypo.domain.entity.ReferenceOpentheso;
import fr.cnrs.opentypo.infrastructure.persistence.CaracteristiquePhysiqueMonnaieRepository;
import fr.cnrs.opentypo.infrastructure.persistence.CaracteristiquePhysiqueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionDetailRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionMonnaieRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionPateRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Charge et mappe les rubriques « Description » et « Caractéristiques physiques » pour l'API REST.
 * Tous les champs sont renvoyés indépendamment du profil typologique (céramique, monnaie, instrumentum).
 */
@Component
@RequiredArgsConstructor
public class EntityApiDetailMapper {

    private final DescriptionRepository descriptionRepository;
    private final DescriptionDetailRepository descriptionDetailRepository;
    private final DescriptionMonnaieRepository descriptionMonnaieRepository;
    private final DescriptionPateRepository descriptionPateRepository;
    private final CaracteristiquePhysiqueRepository caracteristiquePhysiqueRepository;
    private final CaracteristiquePhysiqueMonnaieRepository caracteristiquePhysiqueMonnaieRepository;
    private final ReferenceOpenthesoRepository referenceOpenthesoRepository;

    public List<EntityTextDescriptionDto> loadPresentationDescriptions(Long entityId) {
        List<Description> descriptions = descriptionRepository.findByEntityIdWithLangue(entityId);
        if (descriptions.isEmpty()) {
            return List.of();
        }
        List<EntityTextDescriptionDto> out = new ArrayList<>(descriptions.size());
        for (Description d : descriptions) {
            String lang = d.getLangue() != null ? d.getLangue().getCode() : null;
            out.add(new EntityTextDescriptionDto(lang, d.getValeur()));
        }
        out.sort(Comparator.comparing(EntityTextDescriptionDto::langCode, Comparator.nullsLast(String::compareTo)));
        return Collections.unmodifiableList(out);
    }

    public EntityDescriptionSectionDto loadDescriptionSection(Long entityId, Entity entity) {
        DescriptionDetail detail = descriptionDetailRepository.findByEntity_Id(entityId).orElse(null);
        DescriptionMonnaie monnaie = descriptionMonnaieRepository.findByEntity_Id(entityId).orElse(null);
        CaracteristiquePhysique physique = caracteristiquePhysiqueRepository.findByEntityIdForApi(entityId).orElse(null);
        Map<String, List<ReferenceOpentheso>> refsByCode = groupReferencesByCode(entityId);
        EntityMetadata metadata = entity.getMetadata();

        return new EntityDescriptionSectionDto(
                toRef(physique != null ? physique.getForme() : null),
                detail != null ? detail.getDecors() : null,
                detail != null ? detail.getMarques() : null,
                detail != null ? detail.getMetrologie() : null,
                toRefs(refsByCode.get("FONCTION_USAGE")),
                toRef(entity.getCategorieFonctionnelle()),
                metadata != null ? metadata.getRelationImitation() : null,
                metadata != null ? metadata.getDenominationInstrumentum() : null,
                monnaie != null ? monnaie.getDroit() : null,
                monnaie != null ? monnaie.getLegendeDroit() : null,
                monnaie != null ? monnaie.getRevers() : null,
                monnaie != null ? monnaie.getLegendeRevers() : null);
    }

    public EntityPhysicalCharacteristicsSectionDto loadPhysicalCharacteristicsSection(Long entityId) {
        DescriptionPate pate = descriptionPateRepository.findByEntity_Id(entityId).orElse(null);
        CaracteristiquePhysique physique = caracteristiquePhysiqueRepository.findByEntityIdForApi(entityId).orElse(null);
        CaracteristiquePhysiqueMonnaie monnaie =
                caracteristiquePhysiqueMonnaieRepository.findByEntityIdForApi(entityId).orElse(null);
        Map<String, List<ReferenceOpentheso>> refsByCode = groupReferencesByCode(entityId);

        return new EntityPhysicalCharacteristicsSectionDto(
                pate != null ? pate.getDescription() : null,
                physique != null ? toRef(physique.getMetrologie()) : null,
                monnaie != null ? monnaie.getMetrologie() : null,
                physique != null ? toRef(physique.getMateriaux()) : null,
                monnaie != null ? toRef(monnaie.getMateriaux()) : null,
                physique != null ? toRef(physique.getDimensions()) : null,
                physique != null ? toRef(physique.getTechnique()) : null,
                monnaie != null ? toRef(monnaie.getTechnique()) : null,
                toRefs(refsByCode.get("FABRICATION_FACONNAGE")),
                toRefs(refsByCode.get("CUISSON_POST_CUISSON")),
                toRefs(refsByCode.get("COULEUR_PATE")),
                toRefs(refsByCode.get("NATURE_PATE")),
                toRefs(refsByCode.get("INCLUSIONS")),
                monnaie != null ? toRef(monnaie.getDenomination()) : null,
                monnaie != null ? toRef(monnaie.getValeur()) : null);
    }

    private Map<String, List<ReferenceOpentheso>> groupReferencesByCode(Long entityId) {
        return referenceOpenthesoRepository.findByEntityId(entityId).stream()
                .filter(r -> r.getCode() != null)
                .collect(Collectors.groupingBy(ReferenceOpentheso::getCode));
    }

    private static OpenThesoReferenceDto toRef(ReferenceOpentheso ref) {
        if (ref == null) {
            return null;
        }
        return new OpenThesoReferenceDto(ref.getCode(), ref.getValeur(), ref.getUrl(), ref.getConceptId());
    }

    private static List<OpenThesoReferenceDto> toRefs(List<ReferenceOpentheso> refs) {
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }
        return refs.stream().map(EntityApiDetailMapper::toRef).toList();
    }
}
