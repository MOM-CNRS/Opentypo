package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.domain.entity.Groupe;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.testsupport.UserTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class UtilisateurRepositoryTest {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private GroupeRepository groupeRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findAllWithGroupe_eagerlyLoadsGroupe() {
        Groupe groupe = persistGroupe(GroupEnum.UTILISATEUR);
        Utilisateur utilisateur = persistUtilisateur("with-groupe@example.com", groupe);

        entityManager.flush();
        entityManager.clear();

        Utilisateur loaded = utilisateurRepository.findAllWithGroupe().stream()
                .filter(u -> u.getId().equals(utilisateur.getId()))
                .findFirst()
                .orElseThrow();

        assertNotNull(loaded.getGroupe());
        assertEquals(GroupEnum.UTILISATEUR.getLabel(), loaded.getGroupe().getNom());
    }

    @Test
    void findByIdWithGroupe_loadsGroupeForEdition() {
        Groupe groupe = persistGroupe(GroupEnum.ADMINISTRATEUR_TECHNIQUE);
        Utilisateur utilisateur = persistUtilisateur("admin-load@example.com", groupe);

        entityManager.flush();
        entityManager.clear();

        Optional<Utilisateur> loaded = utilisateurRepository.findByIdWithGroupe(utilisateur.getId());

        assertTrue(loaded.isPresent());
        assertNotNull(loaded.get().getGroupe());
        assertEquals(GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel(), loaded.get().getGroupe().getNom());
    }

    @Test
    void existsByEmailExcludingUserId_ignoresCurrentUser() {
        Groupe groupe = persistGroupe(GroupEnum.UTILISATEUR);
        Utilisateur first = persistUtilisateur("unique@example.com", groupe);
        Utilisateur second = persistUtilisateur("other@example.com", groupe);
        entityManager.flush();

        assertFalse(utilisateurRepository.existsByEmailExcludingUserId("unique@example.com", first.getId()));
        assertTrue(utilisateurRepository.existsByEmailExcludingUserId("unique@example.com", second.getId()));
    }

    private Groupe persistGroupe(GroupEnum groupEnum) {
        return groupeRepository.save(UserTestFixtures.groupe(groupEnum));
    }

    private Utilisateur persistUtilisateur(String email, Groupe groupe) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setNom("Nom");
        utilisateur.setPrenom("Prenom");
        utilisateur.setPasswordHash("hash");
        utilisateur.setGroupe(groupe);
        utilisateur.setActive(true);
        utilisateur.setCreateBy("test");
        utilisateur.setCreateDate(LocalDateTime.now());
        return utilisateurRepository.save(utilisateur);
    }
}
