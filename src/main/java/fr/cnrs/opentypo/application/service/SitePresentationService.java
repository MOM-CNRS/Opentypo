package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.SitePresentation;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.SitePresentationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SitePresentationService {

    private final SitePresentationRepository sitePresentationRepository;
    private final LangueRepository langueRepository;

    public SitePresentation getByLangueCode(String langueCode) {
        if (langueCode == null || langueCode.isEmpty()) {
            langueCode = "fr";
        }
        Optional<SitePresentation> found = sitePresentationRepository.findByLangue_Code(langueCode);
        if (found.isPresent()) {
            return found.get();
        }
        if (!"fr".equalsIgnoreCase(langueCode)) {
            return sitePresentationRepository.findByLangue_Code("fr")
                    .orElseGet(this::createDefaultPresentation);
        }
        return createDefaultPresentation();
    }

    private SitePresentation createDefaultPresentation() {
        SitePresentation p = new SitePresentation();
        p.setTitre("À propos d'Opentypo");
        p.setDescription("Opentypo est une plateforme de recherche et de gestion de typologies archéologiques développée par le CNRS. Cette application permet aux chercheurs de consulter, gérer et enrichir une base de données complète de types archéologiques, facilitant ainsi la recherche scientifique et la documentation du patrimoine archéologique.");
        return p;
    }

    @Transactional
    public SitePresentation save(String langueCode, String titre, String description) {
        Langue langue = langueRepository.findByCode(langueCode);
        if (langue == null) {
            throw new IllegalArgumentException("Langue inexistante : " + langueCode);
        }
        SitePresentation existing = sitePresentationRepository.findByLangue_Code(langueCode).orElse(null);
        SitePresentation toSave = existing != null ? existing : new SitePresentation();
        toSave.setLangue(langue);
        toSave.setTitre(titre);
        toSave.setDescription(description);
        return sitePresentationRepository.save(toSave);
    }
}
