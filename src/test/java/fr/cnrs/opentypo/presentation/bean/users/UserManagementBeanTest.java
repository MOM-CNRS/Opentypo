package fr.cnrs.opentypo.presentation.bean.users;

import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.domain.entity.Groupe;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.GroupeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.NotificationBean;
import fr.cnrs.opentypo.presentation.bean.UserBean;
import fr.cnrs.opentypo.application.service.UtilisateurService;
import fr.cnrs.opentypo.testsupport.UserTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementBeanTest {

    @Mock
    private UserBean currentUserBean;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private GroupeRepository groupeRepository;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private NotificationBean notificationBean;

    @Mock
    private LoginBean loginBean;

    @InjectMocks
    private UserManagementBean userManagementBean;

    private Groupe groupeUtilisateur;
    private Groupe groupeAdminTechnique;

    @BeforeEach
    void setUp() {
        groupeUtilisateur = UserTestFixtures.groupeWithId(1L, GroupEnum.UTILISATEUR);
        groupeAdminTechnique = UserTestFixtures.groupeWithId(2L, GroupEnum.ADMINISTRATEUR_TECHNIQUE);
        userManagementBean.setAvailableGroups(List.of(groupeUtilisateur, groupeAdminTechnique));
    }

    @Test
    void canAccessUserManagement_delegatesToLoginBean() {
        when(loginBean.canAccessUserManagement()).thenReturn(true);
        assertTrue(userManagementBean.canAccessUserManagement());

        when(loginBean.canAccessUserManagement()).thenReturn(false);
        assertFalse(userManagementBean.canAccessUserManagement());
    }

    @Test
    void canEditOrDeleteUsers_isTrueOnlyForAdmins() {
        when(loginBean.isAdminTechniqueOrFonctionnel()).thenReturn(true);
        assertTrue(userManagementBean.canEditOrDeleteUsers());

        when(loginBean.isAdminTechniqueOrFonctionnel()).thenReturn(false);
        assertFalse(userManagementBean.canEditOrDeleteUsers());
    }

    @Test
    void getFormGroups_returnsAllGroupsForAdmin() {
        when(loginBean.isAdminTechniqueOrFonctionnel()).thenReturn(true);

        assertEquals(2, userManagementBean.getFormGroups().size());
    }

    @Test
    void getFormGroups_filtersToUtilisateurGroupForGestionnaire() {
        when(loginBean.isAdminTechniqueOrFonctionnel()).thenReturn(false);

        List<Groupe> groups = userManagementBean.getFormGroups();

        assertEquals(1, groups.size());
        assertEquals(GroupEnum.UTILISATEUR.getLabel(), groups.get(0).getNom());
    }

    @Test
    void getGroupeEtiquette_returnsExpectedCssClasses() {
        assertEquals("role-badge-viewer", userManagementBean.getGroupeEtiquette(groupeUtilisateur));
        assertEquals("role-badge-admin", userManagementBean.getGroupeEtiquette(groupeAdminTechnique));
        assertEquals("role-badge-viewer", userManagementBean.getGroupeEtiquette(null));
    }

    @Test
    void sauvegarderUser_deniesCreateWhenUserCannotAccessModule() {
        when(loginBean.canAccessUserManagement()).thenReturn(false);
        userManagementBean.setNewUser(validNewUser());
        userManagementBean.setSelectedGroupe(groupeUtilisateur);
        userManagementBean.setPasswordConfirmation("secret1");

        userManagementBean.sauvegarderUser();

        verify(notificationBean).showErrorWithUpdate(anyString(), anyString(), anyString());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void sauvegarderUser_deniesEditForNonAdmin() {
        when(loginBean.isAdminTechniqueOrFonctionnel()).thenReturn(false);
        userManagementBean.setEditMode(true);
        userManagementBean.setNewUser(validNewUser());
        userManagementBean.setSelectedGroupe(groupeUtilisateur);

        userManagementBean.sauvegarderUser();

        verify(notificationBean).showErrorWithUpdate(anyString(), anyString(), anyString());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void sauvegarderUser_rejectsInvalidEmail() {
        when(loginBean.canAccessUserManagement()).thenReturn(true);
        Utilisateur form = validNewUser();
        form.setEmail("invalid-email");
        userManagementBean.setNewUser(form);
        userManagementBean.setSelectedGroupe(groupeUtilisateur);
        userManagementBean.setPasswordConfirmation("secret1");

        userManagementBean.sauvegarderUser();

        verify(notificationBean).showErrorWithUpdate(anyString(), anyString(), anyString());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void sauvegarderUser_rejectsPasswordMismatchOnCreate() {
        when(loginBean.canAccessUserManagement()).thenReturn(true);
        userManagementBean.setNewUser(validNewUser());
        userManagementBean.setSelectedGroupe(groupeUtilisateur);
        userManagementBean.setPasswordConfirmation("different");

        userManagementBean.sauvegarderUser();

        verify(notificationBean).showErrorWithUpdate(anyString(), anyString(), anyString());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void sauvegarderUser_rejectsForbiddenGroupForGestionnaire() {
        when(loginBean.canAccessUserManagement()).thenReturn(true);
        when(loginBean.isAdminTechniqueOrFonctionnel()).thenReturn(false);
        userManagementBean.setNewUser(validNewUser());
        userManagementBean.setSelectedGroupe(groupeAdminTechnique);
        userManagementBean.setPasswordConfirmation("secret1");

        userManagementBean.sauvegarderUser();

        verify(notificationBean).showErrorWithUpdate(anyString(), anyString(), anyString());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void sauvegarderUser_createsUserForGestionnaireWithUtilisateurGroup() {
        when(loginBean.canAccessUserManagement()).thenReturn(true);
        when(loginBean.isAdminTechniqueOrFonctionnel()).thenReturn(false);
        when(currentUserBean.getUsername()).thenReturn("Gestionnaire Test");
        when(utilisateurRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(utilisateurService.encodePassword("secret1")).thenReturn("encoded-secret1");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userManagementBean.setNewUser(validNewUser());
        userManagementBean.setSelectedGroupe(groupeUtilisateur);
        userManagementBean.setPasswordConfirmation("secret1");

        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class)) {
            PrimeFaces primeFaces = mock(PrimeFaces.class);
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);

            userManagementBean.sauvegarderUser();
        }

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertEquals("new@example.com", captor.getValue().getEmail());
        assertEquals("encoded-secret1", captor.getValue().getPasswordHash());
        assertEquals(groupeUtilisateur, captor.getValue().getGroupe());
        assertNull(userManagementBean.getNewUser());
    }

    @Test
    void sauvegarderUser_updatesExistingUserAndPasswordWhenProvided() {
        when(loginBean.isAdminTechniqueOrFonctionnel()).thenReturn(true);
        Utilisateur existing = UserTestFixtures.utilisateur(42L, "old@example.com", groupeUtilisateur);
        Utilisateur form = validNewUser();
        form.setId(42L);
        form.setEmail("updated@example.com");
        form.setPasswordHash("newpass");
        userManagementBean.setEditMode(true);
        userManagementBean.setNewUser(form);
        userManagementBean.setSelectedGroupe(groupeUtilisateur);
        userManagementBean.setPasswordConfirmation("newpass");

        when(utilisateurRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(utilisateurService.encodePassword("newpass")).thenReturn("encoded-newpass");
        when(utilisateurRepository.save(existing)).thenReturn(existing);

        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class)) {
            PrimeFaces primeFaces = mock(PrimeFaces.class);
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);

            userManagementBean.sauvegarderUser();
        }

        assertEquals("updated@example.com", existing.getEmail());
        assertEquals("encoded-newpass", existing.getPasswordHash());
        verify(notificationBean).showSuccessWithUpdate(anyString(), anyString(), anyString());
    }

    @Test
    void initEditUser_loadsUserViaFindByIdWithGroupe() {
        when(loginBean.isAdminTechniqueOrFonctionnel()).thenReturn(true);
        Utilisateur listUser = UserTestFixtures.utilisateur(7L, "edit@example.com", groupeUtilisateur);
        Utilisateur loaded = UserTestFixtures.utilisateur(7L, "edit@example.com", groupeUtilisateur);
        when(utilisateurRepository.findByIdWithGroupe(7L)).thenReturn(Optional.of(loaded));
        when(groupeRepository.findById(1L)).thenReturn(Optional.of(groupeUtilisateur));
        when(groupeRepository.findAll()).thenReturn(List.of(groupeUtilisateur, groupeAdminTechnique));

        assertThrows(Exception.class, () -> userManagementBean.initEditUser(listUser));

        verify(utilisateurRepository).findByIdWithGroupe(7L);
        assertTrue(userManagementBean.isEditMode());
        assertNotSame(listUser, userManagementBean.getNewUser());
        assertEquals(7L, userManagementBean.getNewUser().getId());
        assertNull(userManagementBean.getNewUser().getPasswordHash());
    }

    @Test
    void initEditUser_deniedForNonAdmin() throws Exception {
        when(loginBean.isAdminTechniqueOrFonctionnel()).thenReturn(false);
        Utilisateur listUser = UserTestFixtures.utilisateur(7L, "edit@example.com", groupeUtilisateur);

        userManagementBean.initEditUser(listUser);

        verify(notificationBean).showErrorWithUpdate(anyString(), anyString(), anyString());
        verify(utilisateurRepository, never()).findByIdWithGroupe(any());
    }

    @Test
    void supprimerUser_deniedForNonAdmin() {
        when(loginBean.isAdminTechniqueOrFonctionnel()).thenReturn(false);

        userManagementBean.supprimerUser(UserTestFixtures.utilisateurStandard());

        verify(notificationBean).showErrorWithUpdate(anyString(), anyString(), anyString());
        verify(utilisateurRepository, never()).delete(any());
    }

    @Test
    void supprimerUser_deletesUserForAdmin() {
        when(loginBean.isAdminTechniqueOrFonctionnel()).thenReturn(true);
        when(currentUserBean.getUsername()).thenReturn("Admin Test");
        Utilisateur toDelete = UserTestFixtures.utilisateur(99L, "delete@example.com", groupeUtilisateur);
        when(utilisateurRepository.findById(99L)).thenReturn(Optional.of(toDelete));

        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class)) {
            PrimeFaces primeFaces = mock(PrimeFaces.class);
            PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);
            when(primeFaces.ajax()).thenReturn(ajax);

            userManagementBean.supprimerUser(toDelete);
        }

        verify(utilisateurRepository).delete(toDelete);
        verify(utilisateurRepository).findAllWithGroupe();
    }

    @Test
    void initNouveauUser_deniedWhenCannotCreate() throws Exception {
        when(loginBean.canAccessUserManagement()).thenReturn(false);

        userManagementBean.initNouveauUser();

        verify(notificationBean).showError(anyString(), anyString());
        assertNull(userManagementBean.getNewUser());
    }

    private static Utilisateur validNewUser() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("new@example.com");
        utilisateur.setPrenom("Jean");
        utilisateur.setNom("Dupont");
        utilisateur.setPasswordHash("secret1");
        utilisateur.setActive(true);
        return utilisateur;
    }
}
