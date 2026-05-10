package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Parametrage;
import fr.cnrs.opentypo.infrastructure.config.OpentypoArkProperties;
import fr.cnrs.opentypo.infrastructure.persistence.ParametrageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Construit et assigne des identifiants ARK pour les entités de typologie publiées (groupe, série, type).
 * Le NAAN et l'épaule peuvent être surchargés par le paramétrage du référentiel ({@link Parametrage}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArkIdentifierService {

    private static final Pattern NAAN_PATTERN = Pattern.compile("\\d{5,}");
    /** Ancienne valeur par défaut ; traitée comme « non configuré » pour ne pas fabriquer de faux ARK. */
    private static final String LEGACY_PLACEHOLDER_NAAN = "00000";

    private final OpentypoArkProperties properties;
    private final TypeService typeService;
    private final ParametrageRepository parametrageRepository;

    /**
     * Si l'entité est un groupe / série / type, statut publique, sans idArk existant,
     * attribue un ARK du type {@code ark:/NAAN/shoulder-lowercaseType-id}.
     * Sans NAAN et épaule valides (référentiel et/ou configuration application), aucune attribution.
     */
    public void ensureArkIfAbsentForPublishedTypologyEntity(Entity entity) {
        if (!properties.isEnabled() || entity == null) {
            return;
        }
        if (!EntityStatusEnum.PUBLIQUE.name().equals(entity.getStatut())) {
            return;
        }
        if (StringUtils.hasText(entity.getIdArk())) {
            return;
        }
        if (entity.getId() == null || !isTypologyEntity(entity)) {
            return;
        }

        String naan = resolveEffectiveNaan(entity);
        if (!StringUtils.hasText(naan)) {
            log.debug("ARK skipped (NAAN non configuré), entity id={}", entity.getId());
            return;
        }
        if (LEGACY_PLACEHOLDER_NAAN.equals(naan)) {
            log.debug("ARK skipped (NAAN placeholder réservé), entity id={}", entity.getId());
            return;
        }
        if (!NAAN_PATTERN.matcher(naan).matches()) {
            log.debug("ARK skipped (NAAN invalide : '{}'), entity id={}", naan, entity.getId());
            return;
        }

        String shoulder = resolveEffectiveShoulder(entity);
        if (!StringUtils.hasText(shoulder)) {
            log.debug("ARK skipped (épaule non configurée), entity id={}", entity.getId());
            return;
        }
        String typeToken = typeToken(entity);
        String ark = "ark:/" + naan + "/" + shoulder + "-" + typeToken + "-" + entity.getId();
        entity.setIdArk(ark);
        log.info("Assigned ARK {} to entity id={} code={}", ark, entity.getId(), entity.getCode());
    }

    private String resolveEffectiveNaan(Entity typologyEntity) {
        Optional<String> fromRef = referentielParametrage(typologyEntity).map(Parametrage::getArkNaan);
        if (fromRef.isPresent() && StringUtils.hasText(fromRef.get().trim())) {
            return fromRef.get().trim();
        }
        return properties.getNaan() != null ? properties.getNaan().trim() : "";
    }

    private String resolveEffectiveShoulder(Entity typologyEntity) {
        Optional<String> fromRef = referentielParametrage(typologyEntity).map(Parametrage::getArkShoulder);
        if (fromRef.isPresent() && StringUtils.hasText(fromRef.get().trim())) {
            return sanitizeShoulder(fromRef.get().trim());
        }
        String fromApp = properties.getShoulder() != null ? properties.getShoulder().trim() : "";
        return sanitizeShoulder(fromApp);
    }

    private Optional<Parametrage> referentielParametrage(Entity typologyEntity) {
        Entity ref = typeService.findReferenceAncestor(typologyEntity);
        if (ref == null || ref.getId() == null) {
            return Optional.empty();
        }
        return parametrageRepository.findByEntityId(ref.getId());
    }

    private boolean isTypologyEntity(Entity entity) {
        if (entity.getEntityType() == null || entity.getEntityType().getCode() == null) {
            return false;
        }
        String code = entity.getEntityType().getCode();
        return EntityConstants.ENTITY_TYPE_GROUP.equals(code)
                || EntityConstants.ENTITY_TYPE_SERIES.equals(code)
                || EntityConstants.ENTITY_TYPE_TYPE.equals(code);
    }

    private static String typeToken(Entity entity) {
        String code = entity.getEntityType().getCode().trim().toLowerCase(Locale.ROOT);
        return code.replaceAll("[^a-z0-9]+", "");
    }

    private static String sanitizeShoulder(String shoulder) {
        if (shoulder == null) {
            return "";
        }
        return shoulder.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "").replaceAll("-{2,}", "-");
    }
}
