package fr.cnrs.opentypo.presentation.bean.profile;

import fr.cnrs.opentypo.application.dto.UserPermissionItem;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.UserPermission;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.presentation.bean.ApplicationBean;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.NotificationBean;
import fr.cnrs.opentypo.application.service.UtilisateurService;
import fr.cnrs.opentypo.testsupport.UserTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileBeanTest {

    @Mock
    private LoginBean loginBean;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private NotificationBean notificationBean;

    @Mock
    private UserPermissionRepository userPermissionRepository;

    @Mock
    private ApplicationBean applicationBean;

    @InjectMocks
    private ProfileBean profileBean;

    private Utilisateur currentUser;

    @BeforeEach
    void setUp() {
        currentUser = UserTestFixtures.utilisateurStandard();
    }

    @Test
    void getProfilePermissions_returnsEmptyListWhenNotLoggedIn() {
        when(loginBean.getCurrentUser()).thenReturn(null);

        assertTrue(profileBean.getProfilePermissions().isEmpty());
        verify(userPermissionRepository, never()).findByUtilisateurWithEntityAndLabels(any());
    }

    @Test
    void getProfilePermissions_includesHtmlLabelAndPlainText() {
        when(loginBean.getCurrentUser()).thenReturn(currentUser);

        Entity entity = org.mockito.Mockito.mock(Entity.class);
        EntityType entityType = org.mockito.Mockito.mock(EntityType.class);
        when(entity.getEntityType()).thenReturn(entityType);
        when(entityType.getCode()).thenReturn(EntityConstants.ENTITY_TYPE_GROUP);
        when(entity.getCode()).thenReturn("GRP-01");
        when(entity.getId()).thenReturn(100L);
        when(applicationBean.getEntityLabel(entity)).thenReturn("<p>Libellé <strong>riche</strong></p>");
        when(applicationBean.getEntityLabelPlainText(entity)).thenReturn("Libellé riche");

        UserPermission permission = new UserPermission();
        permission.setEntity(entity);
        permission.setRole("Rédacteur");
        when(userPermissionRepository.findByUtilisateurWithEntityAndLabels(currentUser))
                .thenReturn(List.of(permission));

        List<UserPermissionItem> items = profileBean.getProfilePermissions();

        assertEquals(1, items.size());
        UserPermissionItem item = items.get(0);
        assertEquals("<p>Libellé <strong>riche</strong></p>", item.getEntityLabel());
        assertEquals("Libellé riche", item.getEntityLabelPlainText());
        assertEquals("GRP-01", item.getEntityCode());
        assertEquals("Rédacteur", item.getRole());
        assertEquals(100L, item.getEntityId());
    }

    @Test
    void sauvegarder_rejectsWhenUserNotLoggedIn() {
        when(loginBean.getCurrentUser()).thenReturn(null);

        profileBean.sauvegarder();

        verify(notificationBean).showErrorWithUpdate(anyString(), anyString(), anyString());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void sauvegarder_rejectsInvalidEmail() {
        when(loginBean.getCurrentUser()).thenReturn(currentUser);
        when(utilisateurRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        profileBean.setPrenom("Jean");
        profileBean.setNom("Dupont");
        profileBean.setEmail("not-an-email");

        profileBean.sauvegarder();

        verify(notificationBean).showErrorWithUpdate(anyString(), anyString(), anyString());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void sauvegarder_updatesProfileWithoutPasswordChange() {
        when(loginBean.getCurrentUser()).thenReturn(currentUser);
        when(utilisateurRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(utilisateurRepository.existsByEmailExcludingUserId("jean.dupont@example.com", currentUser.getId()))
                .thenReturn(false);
        when(utilisateurRepository.save(currentUser)).thenReturn(currentUser);

        profileBean.setPrenom("Jean");
        profileBean.setNom("Dupont");
        profileBean.setEmail("jean.dupont@example.com");

        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class)) {
            PrimeFaces primeFaces = mock(PrimeFaces.class);
            PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);
            when(primeFaces.ajax()).thenReturn(ajax);

            profileBean.sauvegarder();
        }

        assertEquals("Jean", currentUser.getPrenom());
        assertEquals("Dupont", currentUser.getNom());
        assertEquals("jean.dupont@example.com", currentUser.getEmail());
        verify(loginBean).setCurrentUser(currentUser);
        verify(notificationBean).showSuccessWithUpdate(anyString(), anyString(), anyString());
        verify(utilisateurService, never()).encodePassword(anyString());
    }

    @Test
    void sauvegarder_updatesPasswordWhenProvided() {
        when(loginBean.getCurrentUser()).thenReturn(currentUser);
        when(utilisateurRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(utilisateurRepository.existsByEmailExcludingUserId(currentUser.getEmail(), currentUser.getId()))
                .thenReturn(false);
        when(utilisateurService.encodePassword("newsecret")).thenReturn("encoded-newsecret");
        when(utilisateurRepository.save(currentUser)).thenReturn(currentUser);

        profileBean.setPrenom(currentUser.getPrenom());
        profileBean.setNom(currentUser.getNom());
        profileBean.setEmail(currentUser.getEmail());
        profileBean.setNouveauMotDePasse("newsecret");
        profileBean.setConfirmationMotDePasse("newsecret");

        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class)) {
            PrimeFaces primeFaces = mock(PrimeFaces.class);
            PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);
            when(primeFaces.ajax()).thenReturn(ajax);

            profileBean.sauvegarder();
        }

        assertEquals("encoded-newsecret", currentUser.getPasswordHash());
        verify(utilisateurService).encodePassword("newsecret");
    }

    @Test
    void sauvegarder_rejectsPasswordMismatch() {
        when(loginBean.getCurrentUser()).thenReturn(currentUser);
        when(utilisateurRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

        profileBean.setPrenom(currentUser.getPrenom());
        profileBean.setNom(currentUser.getNom());
        profileBean.setEmail(currentUser.getEmail());
        profileBean.setNouveauMotDePasse("newsecret");
        profileBean.setConfirmationMotDePasse("other");

        profileBean.sauvegarder();

        verify(notificationBean).showErrorWithUpdate(anyString(), anyString(), anyString());
        verify(utilisateurRepository, never()).save(any());
    }
}
