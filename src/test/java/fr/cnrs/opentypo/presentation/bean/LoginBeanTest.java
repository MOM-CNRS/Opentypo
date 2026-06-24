package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.domain.entity.Groupe;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.testsupport.UserTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginBeanTest {

    @Mock
    private UserPermissionRepository userPermissionRepository;

    @InjectMocks
    private LoginBean loginBean;

    @BeforeEach
    void setUp() {
        loginBean.setAuthenticated(false);
        loginBean.setCurrentUser(null);
    }

    @Test
    void canAccessUserManagement_returnsFalseWhenNotAuthenticated() {
        assertFalse(loginBean.canAccessUserManagement());
        verifyNoInteractions(userPermissionRepository);
    }

    @Test
    void canAccessUserManagement_returnsTrueForAdminTechnique() {
        loginBean.setAuthenticated(true);
        loginBean.setCurrentUser(UserTestFixtures.adminTechnique());

        assertTrue(loginBean.canAccessUserManagement());
        verifyNoInteractions(userPermissionRepository);
    }

    @Test
    void canAccessUserManagement_returnsTrueForAdminFonctionnel() {
        loginBean.setAuthenticated(true);
        loginBean.setCurrentUser(UserTestFixtures.adminFonctionnel());

        assertTrue(loginBean.canAccessUserManagement());
        verifyNoInteractions(userPermissionRepository);
    }

    @Test
    void canAccessUserManagement_returnsTrueForGestionnaireReferentiel() {
        Utilisateur user = UserTestFixtures.gestionnaireReferentiel();
        loginBean.setAuthenticated(true);
        loginBean.setCurrentUser(user);
        when(userPermissionRepository.existsByUserIdAndRole(
                user.getId(), PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel()))
                .thenReturn(true);

        assertTrue(loginBean.canAccessUserManagement());
        verify(userPermissionRepository).existsByUserIdAndRole(
                user.getId(), PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel());
    }

    @Test
    void canAccessUserManagement_returnsFalseForStandardUserWithoutReferentielRole() {
        Utilisateur user = UserTestFixtures.utilisateurStandard();
        loginBean.setAuthenticated(true);
        loginBean.setCurrentUser(user);
        when(userPermissionRepository.existsByUserIdAndRole(
                user.getId(), PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel()))
                .thenReturn(false);

        assertFalse(loginBean.canAccessUserManagement());
    }

    @Test
    void isAdminTechniqueOrFonctionnel_returnsFalseForUtilisateurGroup() {
        loginBean.setCurrentUser(UserTestFixtures.utilisateurStandard());

        assertFalse(loginBean.isAdminTechniqueOrFonctionnel());
    }

    @Test
    void isAdminTechnique_detectsAdminTechniqueGroup() {
        Groupe groupe = UserTestFixtures.groupe(GroupEnum.ADMINISTRATEUR_TECHNIQUE);
        Utilisateur user = UserTestFixtures.utilisateur(10L, "tech@example.com", groupe);
        loginBean.setCurrentUser(user);

        assertTrue(loginBean.isAdminTechnique());
        assertTrue(loginBean.isAdminTechniqueOrFonctionnel());
    }

    @Test
    void canCreateOrEdit_returnsTrueOnlyForAuthenticatedAdmin() {
        loginBean.setAuthenticated(false);
        assertFalse(loginBean.canCreateOrEdit());

        loginBean.setAuthenticated(true);
        loginBean.setCurrentUser(UserTestFixtures.utilisateurStandard());
        assertFalse(loginBean.canCreateOrEdit());

        loginBean.setCurrentUser(UserTestFixtures.adminTechnique());
        assertTrue(loginBean.canCreateOrEdit());
    }
}
