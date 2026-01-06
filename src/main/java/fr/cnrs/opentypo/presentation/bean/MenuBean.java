package fr.cnrs.opentypo.presentation.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.MenuModel;

import java.io.Serializable;

@Getter
@Setter
@SessionScoped
@Named(value = "menuBean")
public class MenuBean implements Serializable {

    @Inject
    private LoginBean loginBean;

    private MenuModel model;
    private boolean lastAuthenticatedState = false;

    @PostConstruct
    public void init() {
        buildMenu();
    }

    public void buildMenu() {
        model = new DefaultMenuModel();

        // Menu Accueil - toujours visible
        DefaultMenuItem home = DefaultMenuItem.builder()
                .icon("pi pi-home")
                .value("Accueil")
                .title("Page d'accueil")
                .outcome("/index.xhtml")
                .styleClass("menu-item")
                .build();

        model.getElements().add(home);

        // Autres éléments du menu - uniquement si l'utilisateur est connecté
        boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
        if (isAuthenticated) {
            DefaultMenuItem recherche = DefaultMenuItem.builder()
                    .icon("pi pi-search")
                    .value("Recherche")
                    .title("Rechercher dans le thésaurus")
                    .outcome("/search/search.xhtml?faces-redirect=true")
                    .styleClass("menu-item")
                    .build();

            model.getElements().add(recherche);
        }

        lastAuthenticatedState = isAuthenticated;
    }

    public MenuModel getModel() {
        // Reconstruire le menu seulement si l'état de connexion a changé
        boolean currentAuthState = loginBean != null && loginBean.isAuthenticated();
        if (model == null || currentAuthState != lastAuthenticatedState) {
            buildMenu();
        }
        return model;
    }
}

