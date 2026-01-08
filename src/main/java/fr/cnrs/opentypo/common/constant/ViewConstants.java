package fr.cnrs.opentypo.common.constant;

/**
 * Constantes liées aux vues et à la navigation
 */
public final class ViewConstants {

    private ViewConstants() {
        // Classe utilitaire - pas d'instanciation
    }

    // Paramètres de requête
    public static final String PARAM_SESSION_EXPIRED = "sessionExpired";
    public static final String PARAM_VIEW_EXPIRED = "viewExpired";
    public static final String PARAM_LOGOUT = "logout";
    public static final String PARAM_TRUE = "true";

    // IDs de composants JSF
    public static final String COMPONENT_GROWL = ":growl";
    public static final String COMPONENT_CARDS_CONTAINER = ":cardsContainer";
    public static final String COMPONENT_TREE_WIDGET = ":treeWidget";
    public static final String COMPONENT_SEARCH_FORM = ":searchForm";
}

