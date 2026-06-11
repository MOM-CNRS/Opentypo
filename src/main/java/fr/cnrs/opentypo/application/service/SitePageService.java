package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.SitePageCode;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.SitePage;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.SitePageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SitePageService {

    private final SitePageRepository sitePageRepository;
    private final LangueRepository langueRepository;

    public SitePage getByPageAndLangue(String pageCode, String langueCode) {
        String normalizedPage = normalizePageCode(pageCode);
        String normalizedLang = normalizeLangueCode(langueCode);

        Optional<SitePage> found = sitePageRepository.findByPageCodeAndLangue_Code(normalizedPage, normalizedLang);
        if (found.isPresent()) {
            return found.get();
        }
        if (!"fr".equalsIgnoreCase(normalizedLang)) {
            return sitePageRepository.findByPageCodeAndLangue_Code(normalizedPage, "fr")
                    .orElseGet(() -> createDefaultPage(normalizedPage, normalizedLang));
        }
        return createDefaultPage(normalizedPage, normalizedLang);
    }

    @Transactional
    public SitePage save(String pageCode, String langueCode, String titre, String contenu) {
        String normalizedPage = normalizePageCode(pageCode);
        Langue langue = langueRepository.findByCode(normalizeLangueCode(langueCode));
        if (langue == null) {
            throw new IllegalArgumentException("Langue inexistante : " + langueCode);
        }

        SitePage existing = sitePageRepository.findByPageCodeAndLangue_Code(normalizedPage, langue.getCode()).orElse(null);
        SitePage toSave = existing != null ? existing : new SitePage();
        toSave.setPageCode(normalizedPage);
        toSave.setLangue(langue);
        toSave.setTitre(titre);
        toSave.setContenu(contenu);
        return sitePageRepository.save(toSave);
    }

    private static String normalizePageCode(String pageCode) {
        return SitePageCode.fromCode(pageCode)
                .map(SitePageCode::getCode)
                .orElseThrow(() -> new IllegalArgumentException("Page inconnue : " + pageCode));
    }

    private static String normalizeLangueCode(String langueCode) {
        if (langueCode == null || langueCode.isBlank()) {
            return "fr";
        }
        return langueCode.trim().toLowerCase();
    }

    private SitePage createDefaultPage(String pageCode, String langueCode) {
        SitePage page = new SitePage();
        page.setPageCode(pageCode);
        Langue langue = langueRepository.findByCode(langueCode);
        page.setLangue(langue);

        SitePageCode code = SitePageCode.fromCode(pageCode).orElse(SitePageCode.CONTACT);
        boolean english = "en".equalsIgnoreCase(langueCode);
        switch (code) {
            case CONTACT -> {
                page.setTitre(english ? "Contact" : "Contact");
                page.setContenu(english
                        ? "<p>Contact page content.</p>"
                        : "<p>Contenu de la page contact.</p>");
            }
            case LEGAL -> {
                page.setTitre(english ? "Legal notice" : "Mentions légales");
                page.setContenu(english
                        ? "<p>Legal notice content.</p>"
                        : "<p>Contenu des mentions légales.</p>");
            }
            case ACCESSIBILITY -> {
                page.setTitre(english ? "Accessibility" : "Accessibilité");
                page.setContenu(english
                        ? "<p>Accessibility statement content.</p>"
                        : "<p>Contenu de la déclaration d'accessibilité.</p>");
            }
        }
        return page;
    }
}
