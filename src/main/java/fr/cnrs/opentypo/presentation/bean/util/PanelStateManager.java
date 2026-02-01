package fr.cnrs.opentypo.presentation.bean.util;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Gestionnaire d'état des panneaux pour ApplicationBean
 */
@Getter
@Setter
public class PanelStateManager implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private boolean showCollections = true;
    private boolean showReferencesPanel = false;
    private boolean showReferencePanel = false;
    private boolean showCategoryPanel = false;
    private boolean showGroupePanel = false;
    private boolean showSeriePanel = false;
    private boolean showTypePanel = false;
    private boolean showTreePanel = false;

    /**
     * Affiche uniquement les cartes
     */
    public void showCollections() {
        resetAll();
        this.showCollections = true;
    }

    /**
     * Affiche le panneau référentiel
     */
    public void showCollectionDetail() {
        resetAll();
        this.showReferencesPanel = true;
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
     * Affiche le panneau référentiel (détail d'un référentiel)
     */
    public void showReference() {
        resetAll();
        this.showReferencePanel = true;
        this.showTreePanel = true;
    }

    /**
     * Vérifie si un panneau de détail est affiché
     */
    public boolean isShowDetail() {
        return showReferencesPanel || showReferencePanel || showCategoryPanel || showGroupePanel || showSeriePanel || showTypePanel;
    }

    /**
     * Réinitialise tous les panneaux
     */
    private void resetAll() {
        this.showCollections = false;
        this.showReferencesPanel = false;
        this.showReferencePanel = false;
        this.showCategoryPanel = false;
        this.showGroupePanel = false;
        this.showSeriePanel = false;
        this.showTypePanel = false;
        this.showTreePanel = false;
    }
}

