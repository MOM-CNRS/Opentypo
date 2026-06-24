package fr.cnrs.opentypo.testsupport;

import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.domain.entity.Groupe;
import fr.cnrs.opentypo.domain.entity.Utilisateur;

public final class UserTestFixtures {

    private UserTestFixtures() {
    }

    public static Groupe groupe(String nom) {
        Groupe groupe = new Groupe();
        groupe.setNom(nom);
        return groupe;
    }

    public static Groupe groupe(GroupEnum groupEnum) {
        return groupe(groupEnum.getLabel());
    }

    public static Groupe groupeWithId(Long id, GroupEnum groupEnum) {
        Groupe groupe = groupe(groupEnum);
        groupe.setId(id);
        return groupe;
    }

    public static Utilisateur utilisateur(Long id, String email, Groupe groupe) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setEmail(email);
        utilisateur.setNom("Nom");
        utilisateur.setPrenom("Prenom");
        utilisateur.setPasswordHash("hash");
        utilisateur.setGroupe(groupe);
        utilisateur.setActive(true);
        return utilisateur;
    }

    public static Utilisateur adminTechnique() {
        return utilisateur(1L, "admin@example.com", groupe(GroupEnum.ADMINISTRATEUR_TECHNIQUE));
    }

    public static Utilisateur adminFonctionnel() {
        return utilisateur(2L, "adminf@example.com", groupe(GroupEnum.ADMINISTRATEUR_FONCTIONNEL));
    }

    public static Utilisateur gestionnaireReferentiel() {
        return utilisateur(3L, "gestionnaire@example.com", groupe(GroupEnum.UTILISATEUR));
    }

    public static Utilisateur utilisateurStandard() {
        return utilisateur(4L, "user@example.com", groupe(GroupEnum.UTILISATEUR));
    }
}
