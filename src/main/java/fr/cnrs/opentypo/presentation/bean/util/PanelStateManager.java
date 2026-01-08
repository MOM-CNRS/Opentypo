package fr.cnrs.opentypo.presentation.bean.util;

import lombok.Getter;
import lombok.Setter;

/**
 * Gestionnaire d'état des panneaux pour ApplicationBean
 */
@Getter
@Setter
public class PanelStateManager {
    
    private boolean showCards = true;
    private boolean showReferentielPanel = false;
    private boolean showCategoryPanel = false;
    private boolean showGroupePanel = false;
    private boolean showSeriePanel = false;
    private boolean showTypePanel = false;
    private boolean showTreePanel = false;

    /**
     * Affiche uniquement les cartes
     */
    public void showCards() {
        resetAll();
        this.showCards = true;
    }

    /**
     * Affiche le panneau référentiel
     */
    public void showReferentiel() {
        resetAll();
        this.showReferentielPanel = true;
        this.showTreePanel = true;
    }

    /**
     * Affiche le panneau catégorie
     */
    public void showCategory() {
        resetAll();
        this.showCategoryPanel = true;
        this.showTreePanel = true;
    }

    /**
     * Affiche le panneau groupe
     */
    public void showGroupe() {
        resetAll();
        this.showGroupePanel = true;
        this.showTreePanel = true;
    }

    /**
     * Affiche le panneau série
     */
    public void showSerie() {
        resetAll();
        this.showSeriePanel = true;
        this.showTreePanel = true;
    }

    /**
     * Affiche le panneau type
     */
    public void showType() {
        resetAll();
        this.showTypePanel = true;
        this.showTreePanel = true;
    }

    /**
     * Vérifie si un panneau de détail est affiché
     */
    public boolean isShowDetail() {
        return showReferentielPanel || showCategoryPanel || showGroupePanel 
            || showSeriePanel || showTypePanel;
    }

    /**
     * Réinitialise tous les panneaux
     */
    private void resetAll() {
        this.showCards = false;
        this.showReferentielPanel = false;
        this.showCategoryPanel = false;
        this.showGroupePanel = false;
        this.showSeriePanel = false;
        this.showTypePanel = false;
        this.showTreePanel = false;
    }
}

