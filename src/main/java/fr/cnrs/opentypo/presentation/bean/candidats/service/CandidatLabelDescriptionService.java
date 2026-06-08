package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryDescriptionItem;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryLabelItem;
import fr.cnrs.opentypo.presentation.i18n.JsfMessages;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service pour l'ajout/suppression des labels et descriptions dans le formulaire candidat.
 */
@Service
@Slf4j
public class CandidatLabelDescriptionService {

    @Inject private EntityRepository entityRepository;
    @Inject private LangueRepository langueRepository;

    public record AddLabelResult(boolean success, CategoryLabelItem addedItem, String errorMessage) {}
    public record RemoveLabelResult(boolean success, String errorMessage) {}
    public record AddDescriptionResult(boolean success, CategoryDescriptionItem addedItem, String errorMessage) {}
    public record RemoveDescriptionResult(boolean success, String errorMessage) {}

    @Transactional
    public AddLabelResult addLabel(Long entityId, String newLabelValue, String newLabelLangueCode, String principalLangueCode) {

        if (entityId == null) return new AddLabelResult(false, null, JsfMessages.get("candidat.labelDesc.entityNotCreatedYet"));
        if (newLabelValue == null || newLabelValue.trim().isEmpty()) return new AddLabelResult(false, null, JsfMessages.get("modifier.msg.labelRequired"));
        if (newLabelLangueCode == null || newLabelLangueCode.trim().isEmpty()) return new AddLabelResult(false, null, JsfMessages.get("modifier.msg.languageRequired"));
        if (principalLangueCode != null && principalLangueCode.equals(newLabelLangueCode)) {
            return new AddLabelResult(false, null, JsfMessages.get("candidat.labelDesc.langUsedForPrincipal"));
        }

        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null) return new AddLabelResult(false, null, JsfMessages.get("candidat.error.entityNotFoundDb"));

        if (entity.getLabels() != null) {
            boolean used = entity.getLabels().stream()
                    .anyMatch(l -> l.getLangue() != null && l.getLangue().getCode() != null
                            && l.getLangue().getCode().equals(newLabelLangueCode));
            if (used) return new AddLabelResult(false, null, JsfMessages.get("candidat.labelDesc.langUsedForOtherLabel"));
        }

        Langue langue = langueRepository.findByCode(newLabelLangueCode);
        if (langue == null) return new AddLabelResult(false, null, JsfMessages.get("candidat.labelDesc.langNotFound"));

        Label label = new Label();
        label.setNom(newLabelValue.trim());
        label.setEntity(entity);
        label.setLangue(langue);
        if (entity.getLabels() == null) entity.setLabels(new ArrayList<>());
        entity.getLabels().add(label);
        entityRepository.save(entity);

        return new AddLabelResult(true, new CategoryLabelItem(newLabelValue.trim(), newLabelLangueCode, langue), null);
    }

    @Transactional
    public RemoveLabelResult removeLabel(Long entityId, CategoryLabelItem labelItem) {
        if (entityId == null) return new RemoveLabelResult(false, JsfMessages.get("candidat.labelDesc.entityNotCreatedYet"));

        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null || entity.getLabels() == null) return new RemoveLabelResult(false, null);

        Label toRemove = entity.getLabels().stream()
                .filter(l -> l.getLangue() != null && l.getLangue().getCode() != null
                        && l.getLangue().getCode().equals(labelItem.getLangueCode())
                        && l.getNom() != null && l.getNom().equals(labelItem.getNom()))
                .findFirst().orElse(null);

        if (toRemove != null) {
            entity.getLabels().remove(toRemove);
            entityRepository.save(entity);
        }
        return new RemoveLabelResult(true, null);
    }

    public boolean isLangueAlreadyUsedInLabels(String langueCode, CategoryLabelItem currentItem,
                                               List<CategoryLabelItem> candidatLabels, Entity entity) {
        if (langueCode == null || langueCode.isEmpty()) return false;
        if (candidatLabels != null) {
            if (candidatLabels.stream().filter(i -> i != currentItem && i.getLangueCode() != null)
                    .anyMatch(i -> i.getLangueCode().equals(langueCode))) return true;
        }
        if (entity != null && entity.getLabels() != null) {
            return entity.getLabels().stream()
                    .anyMatch(l -> l.getLangue() != null && l.getLangue().getCode() != null
                            && l.getLangue().getCode().equals(langueCode));
        }
        return false;
    }

    public boolean isLangueAlreadyUsedInLabels(String langueCode, List<CategoryLabelItem> candidatLabels) {
        if (langueCode == null || langueCode.isEmpty() || candidatLabels == null) return false;
        return candidatLabels.stream()
                .filter(element -> element.getLangueCode() != null)
                .anyMatch(element -> element.getLangueCode().equalsIgnoreCase(langueCode));
    }

    @Transactional
    public AddDescriptionResult addDescription(Long entityId, String newDescValue, String newDescLangueCode,
                                               List<CategoryDescriptionItem> existingDescriptions) {
        if (entityId == null) return new AddDescriptionResult(false, null, JsfMessages.get("candidat.labelDesc.entityNotCreatedYet"));
        if (newDescValue == null || newDescValue.trim().isEmpty()) return new AddDescriptionResult(false, null, JsfMessages.get("modifier.msg.descriptionRequired"));
        if (newDescLangueCode == null || newDescLangueCode.trim().isEmpty()) return new AddDescriptionResult(false, null, JsfMessages.get("modifier.msg.languageRequired"));

        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null) return new AddDescriptionResult(false, null, JsfMessages.get("candidat.error.entityNotFoundDb"));

        if (entity.getDescriptions() != null) {
            boolean used = entity.getDescriptions().stream()
                    .anyMatch(d -> d.getLangue() != null && d.getLangue().getCode() != null
                            && d.getLangue().getCode().equals(newDescLangueCode));
            if (used) return new AddDescriptionResult(false, null, JsfMessages.get("candidat.labelDesc.langUsedForOtherDescription"));
        }

        Langue langue = langueRepository.findByCode(newDescLangueCode);
        if (langue == null) return new AddDescriptionResult(false, null, JsfMessages.get("candidat.labelDesc.langNotFound"));

        Description desc = new Description();
        desc.setValeur(newDescValue.trim());
        desc.setEntity(entity);
        desc.setLangue(langue);
        if (entity.getDescriptions() == null) entity.setDescriptions(new ArrayList<>());
        entity.getDescriptions().add(desc);
        entityRepository.save(entity);

        return new AddDescriptionResult(true, new CategoryDescriptionItem(newDescValue.trim(), newDescLangueCode, langue), null);
    }

    @Transactional
    public RemoveDescriptionResult removeDescription(Long entityId, CategoryDescriptionItem descItem) {
        if (entityId == null) return new RemoveDescriptionResult(false, JsfMessages.get("candidat.labelDesc.entityNotCreatedYet"));

        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null || entity.getDescriptions() == null) return new RemoveDescriptionResult(false, null);

        Description toRemove = entity.getDescriptions().stream()
                .filter(d -> d.getLangue() != null && d.getLangue().getCode() != null
                        && d.getLangue().getCode().equals(descItem.getLangueCode())
                        && d.getValeur() != null && d.getValeur().equals(descItem.getValeur()))
                .findFirst().orElse(null);

        if (toRemove != null) {
            entity.getDescriptions().remove(toRemove);
            entityRepository.save(entity);
        }
        return new RemoveDescriptionResult(true, null);
    }

    public boolean isLangueAlreadyUsedInDescriptions(String langueCode, CategoryDescriptionItem currentItem,
                                                    List<CategoryDescriptionItem> descriptions, Entity entity) {
        if (langueCode == null || langueCode.isEmpty()) return false;
        if (descriptions != null) {
            if (descriptions.stream().filter(d -> d != currentItem && d.getLangueCode() != null)
                    .anyMatch(d -> d.getLangueCode().equals(langueCode))) return true;
        }
        if (entity != null && entity.getDescriptions() != null) {
            return entity.getDescriptions().stream()
                    .anyMatch(d -> d.getLangue() != null && d.getLangue().getCode() != null
                            && d.getLangue().getCode().equals(langueCode));
        }
        return false;
    }

    public boolean isLangueAlreadyUsedInDescriptions(String langueCode, List<CategoryDescriptionItem> descriptions) {
        if (langueCode == null || langueCode.isEmpty() || descriptions == null) return false;

        return descriptions.stream()
                .filter(element -> element.getLangueCode() != null)
                .anyMatch(element -> element.getLangueCode().equalsIgnoreCase(langueCode));
    }
}
